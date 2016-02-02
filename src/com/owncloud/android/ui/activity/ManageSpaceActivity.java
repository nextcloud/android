package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
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

import java.io.File;

public class ManageSpaceActivity extends AppCompatActivity {

    private static final String TAG = ManageSpaceActivity.class.getSimpleName();

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
                clearData();
            }
        });
    }

    /**
     * Save passcode from Share preferences
     * Clear the rest of data
     */
    private void clearData() {
        // Save passcode from Share preferences
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        boolean passCodeEnable = appPrefs.getBoolean("set_pincode", false);

        String passCodeDigits[] = new String[4];
        if (passCodeEnable) {
            passCodeDigits[0] = appPrefs.getString("PrefPinCode1", null);
            passCodeDigits[1] = appPrefs.getString("PrefPinCode2", null);
            passCodeDigits[2] = appPrefs.getString("PrefPinCode3", null);
            passCodeDigits[3] = appPrefs.getString("PrefPinCode4", null);
        }

        // Clear data
        clearApplicationData();

        // Recover passcode
        SharedPreferences.Editor appPrefsEditor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        if (passCodeEnable) {
            appPrefsEditor.putString("PrefPinCode1", passCodeDigits[0]);
            appPrefsEditor.putString("PrefPinCode2", passCodeDigits[1]);
            appPrefsEditor.putString("PrefPinCode3", passCodeDigits[2]);
            appPrefsEditor.putString("PrefPinCode4", passCodeDigits[3]);
        }

        appPrefsEditor.putBoolean("set_pincode", passCodeEnable);
        appPrefsEditor.commit();


        finish();
        System.exit(0);

    }

    public void clearApplicationData() {
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib")) {
                    File fileToDelete = new File(appDir, s);
                    deleteDir(fileToDelete);
                    Log_OC.d(TAG, "Clear Application Data, File: " + fileToDelete.getName()+ " DELETED *******");
                }
            }
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                Log_OC.w(TAG, "Unknown menu item triggered");
                return false;
        }
        return true;
    }
}
