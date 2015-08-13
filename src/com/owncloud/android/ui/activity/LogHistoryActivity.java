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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;


public class LogHistoryActivity extends AppCompatActivity {

    private static final String MAIL_ATTACHMENT_TYPE = "text/plain";

    private static final String KEY_LOG_TEXT = "LOG_TEXT";

    private static final String TAG = LogHistoryActivity.class.getSimpleName();

    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";

    private String mLogPath = FileStorageUtils.getLogPath();
    private File logDIR = null;
    private String mLogText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.log_send_file);
        setTitle(getText(R.string.actionbar_logger));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Button deleteHistoryButton = (Button) findViewById(R.id.deleteLogHistoryButton);
        Button sendHistoryButton = (Button) findViewById(R.id.sendLogHistoryButton);
        TextView logTV = (TextView) findViewById(R.id.logTV);

        deleteHistoryButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {

                Log_OC.deleteHistoryLogging();
                finish();
            }
        });

        sendHistoryButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMail();
            }
        });

        if (savedInstanceState == null) {
            if (mLogPath != null) {
                logDIR = new File(mLogPath);
            }

            if (logDIR != null && logDIR.isDirectory()) {
                // Show a dialog while log data is being loaded
                showLoadingDialog();

                // Start a new thread that will load all the log data
                LoadingLogTask task = new LoadingLogTask(logTV);
                task.execute();
            }
        } else {
            mLogText = savedInstanceState.getString(KEY_LOG_TEXT);
            logTV.setText(mLogText);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
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

        // For the moment we need to consider the possibility that setup.xml
        // does not include the "mail_logger" entry. This block prevents that
        // compilation fails in this case.
        String emailAddress;
        try {
            Class<?> stringClass = R.string.class;
            Field mailLoggerField = stringClass.getField("mail_logger");
            int emailAddressId = (Integer) mailLoggerField.get(null);
            emailAddress = getString(emailAddressId);
        } catch (Exception e) {
            emailAddress = "";
        }
        
        ArrayList<Uri> uris = new ArrayList<Uri>();

        // Convert from paths to Android friendly Parcelable Uri's
        for (String file : Log_OC.getLogFileNames())
        {
            File logFile = new File(mLogPath, file);
            if (logFile.exists()) {
                uris.add(Uri.fromFile(logFile));
            }
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        intent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        String subject = String.format(getString(R.string.log_send_mail_subject), getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType(MAIL_ATTACHMENT_TYPE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.log_send_no_mail_app), Toast.LENGTH_LONG).show();
            Log_OC.i(TAG, "Could not find app for sending log history.");
        }

    }

    /**
     *
     * Class for loading the log data async
     *
     */
    private class LoadingLogTask extends AsyncTask<String, Void, String> {
        private final WeakReference<TextView> textViewReference;

        public LoadingLogTask(TextView logTV){
            // Use of a WeakReference to ensure the TextView can be garbage collected
            textViewReference  = new WeakReference<TextView>(logTV);
        }

        protected String doInBackground(String... args) {
            return readLogFile();
        }

        protected void onPostExecute(String result) {
            if (textViewReference != null && result != null) {
                final TextView logTV = textViewReference.get();
                if (logTV != null) {
                    mLogText = result;
                    logTV.setText(mLogText);
                    dismissLoadingDialog();
                }
            }
        }

        /**
         * Read and show log file info
         */
        private String readLogFile() {

            String[] logFileName = Log_OC.getLogFileNames();

            //Read text from files
            StringBuilder text = new StringBuilder();

            BufferedReader br = null;
            try {
                String line;

                for (int i = logFileName.length-1; i >= 0; i--) {
                    File file = new File(mLogPath,logFileName[i]);
                    if (file.exists()) {
                        // Check if FileReader is ready
                        if (new FileReader(file).ready()) {
                            br = new BufferedReader(new FileReader(file));
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
                
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            return text.toString();
        }
   }

    /**
     * Show loading dialog
     */
    public void showLoadingDialog() {
        // Construct dialog
        LoadingDialog loading = new LoadingDialog(
                getResources().getString(R.string.log_progress_dialog_text)
        );
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        loading.show(ft, DIALOG_WAIT_TAG);
    }

    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog(){
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag != null) {
            LoadingDialog loading = (LoadingDialog) frag;
            loading.dismiss();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /// global state
        outState.putString(KEY_LOG_TEXT, mLogText);
    }
}