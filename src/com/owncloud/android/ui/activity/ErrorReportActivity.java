/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;


public class ErrorReportActivity extends AppCompatActivity {

    private static final String TAG = ErrorReportActivity.class.getSimpleName();

    private String mLogText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.error_send);
        setTitle(getString(R.string.error_log_title));
        Button cancelErrorLogButton = (Button) findViewById(R.id.cancelErrorLogButton);
        Button sendErrorLogButton = (Button) findViewById(R.id.sendErrorLogButton);
        TextView logTV = (TextView) findViewById(R.id.logTV);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            mLogText = intent.getStringExtra(Intent.EXTRA_TEXT);
        } else {
            // Handle other intents, such as being started from the home screen
            mLogText = "Error, nothing received!";
        }

        logTV.setText(mLogText);

        cancelErrorLogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();

            }
        });

        sendErrorLogButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMail();
            }
        });
    }

    /**
     * Start activity for sending email with logs attached
     */
    private void sendMail() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mLogText);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}