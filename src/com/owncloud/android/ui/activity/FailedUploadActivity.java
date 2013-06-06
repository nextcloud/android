/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.owncloud.android.R;

/**
 * This Activity is used to display a detail message for failed uploads
 * 
 * The entry-point for this activity is the 'Failed upload Notification"
 * 
 * @author andomaex / Matthias Baumann
 */
public class FailedUploadActivity extends Activity {

    public static final String MESSAGE = "message";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.failed_upload_message_view);
        String message = getIntent().getStringExtra(MESSAGE);
        TextView textView = (TextView) findViewById(R.id.faild_upload_message);
        textView.setText(message);
        Button close_button = (Button) findViewById(R.id.failed_uploadactivity_close_button);
        close_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
