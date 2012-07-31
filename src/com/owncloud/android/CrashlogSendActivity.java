/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

package com.owncloud.android;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;

import com.owncloud.android.R;

public class CrashlogSendActivity extends SherlockActivity implements OnClickListener, OnCancelListener {
    
    private static final String TAG = "CrashlogSendActivity";
    private static final String CRASHLOG_SUBMIT_URL = "http://alefzero.eu/a/crashlog/";
    private static final int DIALOG_SUBMIT = 5;
    
    private String mLogFilename;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogFilename = getIntent().getStringExtra(CrashHandler.KEY_CRASH_FILENAME);
        if (mLogFilename == null) {
            Log.wtf(TAG, "No file crashlog path given!");
            finish();
            return;
        }
        Log.i(TAG, "crashlog file path " + mLogFilename);
        
        showDialog(DIALOG_SUBMIT);
    }
    

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_SUBMIT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.crashlog_message);
            builder.setNegativeButton(R.string.crashlog_dont_send_report, this);
            builder.setPositiveButton(R.string.crashlog_send_report, this);
            builder.setCancelable(true);
            builder.setOnCancelListener(this);
            return builder.create();
        }
        return super.onCreateDialog(id);
    }


    @Override
    public void onClick(DialogInterface dialog, final int which) {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                // TODO Auto-generated method stub
                File file = new File(mLogFilename);
                if (which == Dialog.BUTTON_POSITIVE) {
                    try {
                        HttpClient client = new HttpClient();
                        PostMethod post = new PostMethod(CRASHLOG_SUBMIT_URL);
                        Part[] parts = {new FilePart("crashfile", file)};
                        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
                        client.executeMethod(post);
                        post.releaseConnection();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                file.delete();
                finish();                
            }
        }).start();
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        new File(mLogFilename).delete();
        finish();
    }

}
