/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2016 Nextcloud
 * Copyright (C) 2016 Tobias Kaminsky
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.InstantUploadPreference;
import com.owncloud.android.lib.common.utils.Log_OC;

public class InstantUploadPreferencesActivity extends ToolbarActivity {
    private TextView mLocalPath;
    private TextView mRemotePath;
    private CheckBox mOnlyCharging;
    private CheckBox mOnlyWiFi;
    private static final String TAG = "InstantUploadPreferencesActivity";
    public final static String NUMBER = "InstantUploadPreferences.Number";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.instantupload_preferences);

        Intent intent = getIntent();
        String message = intent.getStringExtra(NUMBER);
        Log_OC.d(TAG, "number: " + message);

        final InstantUploadPreference preference = new InstantUploadPreference(getApplicationContext(), message);

        // Action bar setup
        setupToolbar();
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // TODO Tobi: Local path chooseable with dialog
        mLocalPath = (TextView) findViewById(R.id.localPath);
        mLocalPath.setText(preference.getLocalPath());

        // TODO Tobi: remote path chooseable with dialog
        mRemotePath = (TextView) findViewById(R.id.remotePath);
        mRemotePath.setText(preference.getRemotePath());

        mOnlyWiFi = (CheckBox) findViewById(R.id.onlyWifi);
        mOnlyWiFi.setChecked(preference.isOnlyWiFi());

        mOnlyCharging = (CheckBox) findViewById(R.id.onlyCharging);
        mOnlyCharging.setChecked(preference.isOnlyCharging());

        Button save = (Button) findViewById(R.id.instantUpload_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preference.setLocalPath(mLocalPath.getText().toString());
                preference.setRemotePath(mRemotePath.getText().toString());
                preference.setOnlyWiFi(mOnlyWiFi.isChecked());
                preference.setOnlyCharging(mOnlyCharging.isChecked());

                if (preference.saveAll()) {
                    Intent preferencesIntent = new Intent(MainApp.getAppContext(), Preferences.class);
                    startActivity(preferencesIntent);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                // TODO: also persist on back
                onBackPressed();
            }
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }
}