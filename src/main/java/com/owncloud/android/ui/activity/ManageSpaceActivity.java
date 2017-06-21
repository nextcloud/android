/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2016 ownCloud Inc.
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

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.AnalyticsUtils;

import java.io.File;

public class ManageSpaceActivity extends AppCompatActivity {

    private static final String TAG = ManageSpaceActivity.class.getSimpleName();

    private static final String LIB_FOLDER = "lib";

    private static final String SCREEN_NAME = "Manage space";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_space);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.manage_space_title);

        TextView descriptionTextView = (TextView) findViewById(R.id.general_description);
        descriptionTextView.setText(getString(R.string.manage_space_description, getString(R.string.app_name)));

        Button clearDataButton = (Button) findViewById(R.id.clearDataButton);
        clearDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClearDataAsynTask clearDataTask = new ClearDataAsynTask();
                clearDataTask.execute();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                Log_OC.w(TAG, "Unknown menu item triggered");
                retval =  super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    /**
     * AsyncTask for Clear Data, saving the passcode
     */
    private class ClearDataAsynTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... params) {

            boolean result = true;

            // Save passcode from Share preferences
            SharedPreferences appPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());

            boolean passCodeEnable = appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);

            String passCodeDigits[] = new String[4];
            if (passCodeEnable) {
                passCodeDigits[0] = appPrefs.getString(PassCodeActivity.PREFERENCE_PASSCODE_D1, null);
                passCodeDigits[1] = appPrefs.getString(PassCodeActivity.PREFERENCE_PASSCODE_D2, null);
                passCodeDigits[2] = appPrefs.getString(PassCodeActivity.PREFERENCE_PASSCODE_D3, null);
                passCodeDigits[3] = appPrefs.getString(PassCodeActivity.PREFERENCE_PASSCODE_D4, null);
            }

            // Clear data
            result = clearApplicationData();


            // Clear SharedPreferences
            SharedPreferences.Editor appPrefsEditor = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()).edit();
            appPrefsEditor.clear();
            result = result && appPrefsEditor.commit();

            // Recover passcode
            if (passCodeEnable) {
                appPrefsEditor.putString(PassCodeActivity.PREFERENCE_PASSCODE_D1, passCodeDigits[0]);
                appPrefsEditor.putString(PassCodeActivity.PREFERENCE_PASSCODE_D2, passCodeDigits[1]);
                appPrefsEditor.putString(PassCodeActivity.PREFERENCE_PASSCODE_D3, passCodeDigits[2]);
                appPrefsEditor.putString(PassCodeActivity.PREFERENCE_PASSCODE_D4, passCodeDigits[3]);
            }

            appPrefsEditor.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, passCodeEnable);
            result = result && appPrefsEditor.commit();

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.manage_space_clear_data),
                        Toast.LENGTH_LONG).show();
            } else {
                finish();
                System.exit(0);
            }

        }

        public boolean clearApplicationData() {
            boolean clearResult = true;
            File appDir = new File(getCacheDir().getParent());
            if (appDir.exists()) {
                String[] children = appDir.list();
                if (children != null) {
                    for (String s : children) {
                        if (!LIB_FOLDER.equals(s)) {
                            File fileToDelete = new File(appDir, s);
                            clearResult = clearResult && deleteDir(fileToDelete);
                            Log_OC.d(TAG, "Clear Application Data, File: " + fileToDelete.getName() + " DELETED *****");
                        }
                    }
                } else {
                    clearResult = false;
                }
            }
            return  clearResult;
        }

        public boolean deleteDir(File dir) {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                if (children != null) {
                    for (int i = 0; i < children.length; i++) {
                        boolean success = deleteDir(new File(dir, children[i]));
                        if (!success) {
                            Log_OC.w(TAG, "File NOT deleted " + children[i]);
                            return false;
                        } else {
                            Log_OC.d(TAG, "File deleted " + children[i]);
                        }
                    }
                } else {
                    return false;
                }
            }

            return dir.delete();
        }
    }
}
