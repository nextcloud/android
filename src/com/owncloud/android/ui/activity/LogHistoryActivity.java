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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.Log_OC;


public class LogHistoryActivity extends SherlockActivity {

    private static final String MAIL_ATTACHMENT_TYPE = "text/plain";

    private static final String TAG = LogHistoryActivity.class.getSimpleName();

    private String mLogPath = FileStorageUtils.getLogPath();
    private File logDIR = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.log_send_file);
        setTitle(getText(R.string.actionbar_logger));
        ActionBar actionBar = getSherlock().getActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());
        actionBar.setDisplayHomeAsUpEnabled(true);
        Button deleteHistoryButton = (Button) findViewById(R.id.deleteLogHistoryButton);
        Button sendHistoryButton = (Button) findViewById(R.id.sendLogHistoryButton);

        deleteHistoryButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {

                Log_OC.stopLogging();
                finish();
            }
        });

        sendHistoryButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMail();
            }
        });

        if(mLogPath != null){
        logDIR = new File(mLogPath);
        }

        if(logDIR != null && logDIR.isDirectory()) {
            readLogFile();
        }
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        default:
            return false;
        }
        return true;
    }


    /**
     * Start activity for sending email with logs attached
     */
    private void sendMail() {

        String emailAddresses[] = { getText(R.string.mail_logger).toString() };

        ArrayList<Uri> uris = new ArrayList<Uri>();

        // Convert from paths to Android friendly Parcelable Uri's
        for (String file : Log_OC.getLogFileNames())
        {
            if (new File(mLogPath + File.separator, file).exists()) {
                Uri u = Uri.parse("file://" + mLogPath + File.separator + file);
                uris.add(u);
            }
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        // Explicitly only use Gmail to send
        intent.setClassName("com.google.android.gm","com.google.android.gm.ComposeActivityGmail");
        intent.putExtra(Intent.EXTRA_EMAIL, emailAddresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.log_mail_subject));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType(MAIL_ATTACHMENT_TYPE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }


    /**
     * Read and show log file info
     */
    private void readLogFile() {

        String[] logFileName = Log_OC.getLogFileNames();

        //Read text from files
        StringBuilder text = new StringBuilder();

        try {

            String line;

            for (int i = logFileName.length-1; i >= 0; i--) {
                File file = new File(mLogPath,logFileName[i]);
                if (file.exists()) {
                    // Check if FileReader is ready
                    if (new FileReader(file).ready()) {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        while ((line = br.readLine()) != null) {
                            // Append the log info
                            text.append(line);
                            text.append('\n');
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            Log_OC.d(TAG, e.getMessage().toString());
        }

        // Show in the screen the content of the log
        TextView logTV = (TextView) findViewById(R.id.logTV);
        logTV.setText(text);
    }
}