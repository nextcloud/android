/*
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

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;

import javax.inject.Inject;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class ManageSpaceActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = ManageSpaceActivity.class.getSimpleName();
    private static final String LIB_FOLDER = "lib";

    @Inject AppPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_space);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.manage_space_title);
        }

        TextView descriptionTextView = findViewById(R.id.general_description);
        descriptionTextView.setText(getString(R.string.manage_space_description, getString(R.string.app_name)));

        Button clearDataButton = findViewById(R.id.clearDataButton);
        clearDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClearDataAsyncTask clearDataTask = new ClearDataAsyncTask();
                clearDataTask.execute();
            }
        });
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
    private class ClearDataAsyncTask extends AsyncTask<Void, Void, Boolean>{

        private final AppPreferences preferences = ManageSpaceActivity.this.preferences;

        @Override
        protected Boolean doInBackground(Void... params) {

            String lockPref = preferences.getLockPreference();
            boolean passCodeEnable = SettingsActivity.LOCK_PASSCODE.equals(lockPref);

            String passCodeDigits[] = new String[4];
            if (passCodeEnable) {
                passCodeDigits = preferences.getPassCode();
            }

            // Clear data
            preferences.clear();
            boolean result = clearApplicationData();

            // Recover passcode
            if (passCodeEnable) {
                preferences.setPassCode(
                    passCodeDigits[0],
                    passCodeDigits[1],
                    passCodeDigits[2],
                    passCodeDigits[3]
                );
            }

            preferences.setLockPreference(lockPref);

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.manage_space_clear_data,
                        Snackbar.LENGTH_LONG
                ).show();
            } else {
                finish();
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
                    for (String child : children) {
                        boolean success = deleteDir(new File(dir, child));
                        if (!success) {
                            Log_OC.w(TAG, "File NOT deleted " + child);
                            return false;
                        } else {
                            Log_OC.d(TAG, "File deleted " + child);
                        }
                    }
                } else {
                    return false;
                }
            }

            if (dir != null) {
                return dir.delete();
            } else {
                return false;
            }
        }
    }
}
