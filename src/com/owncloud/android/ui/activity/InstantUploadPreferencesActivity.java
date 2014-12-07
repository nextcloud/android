package com.owncloud.android.ui.activity;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.InstantUploadPreference;
import com.owncloud.android.lib.common.utils.Log_OC;

public class InstantUploadPreferencesActivity extends Activity {
    TextView pLocalPath;
    TextView pRemotePath;
    CheckBox pOnlyCharging;
    CheckBox pOnlyWiFi;
    SharedPreferences sharedPref;
    private static final String TAG = "InstantUploadPreferencesActivity";
    public final static String NUMBER = "InstantUploadPreferences.Number";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.instantupload_preferences);

        Intent intent = getIntent();
        String message = intent.getStringExtra(NUMBER);
        Log_OC.d(TAG, "number: " + message);

        final InstantUploadPreference preference = new InstantUploadPreference(getApplicationContext(), message);

        pLocalPath = (TextView) findViewById(R.id.localPath);
        pLocalPath.setText(preference.getLocalPath());

        pRemotePath = (TextView) findViewById(R.id.remotePath);                
        pRemotePath.setText(preference.getRemotePath());

        pOnlyWiFi = (CheckBox) findViewById(R.id.onlyWifi);
        pOnlyWiFi.setChecked(preference.isOnlyWiFi());

        pOnlyCharging = (CheckBox) findViewById(R.id.onlyCharging);
        pOnlyCharging.setChecked(preference.isOnlyCharging());

        Button save = (Button) findViewById(R.id.instantUpload_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preference.setLocalPath(pLocalPath.getText().toString());
                preference.setRemotePath(pRemotePath.getText().toString());
                preference.setOnlyWiFi(pOnlyWiFi.isChecked());
                preference.setOnlyCharging(pOnlyCharging.isChecked());

                if (preference.saveAll()){
                    Intent preferencesIntent = new Intent(MainApp.getAppContext(), Preferences.class);
                    startActivity(preferencesIntent);
                }
            }
        });

    }


}