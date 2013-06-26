/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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

import java.io.File;
import java.util.Vector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.Log_OC;
import com.owncloud.android.OwnCloudSession;
import com.owncloud.android.R;
import com.owncloud.android.db.DbHandler;

/**
 * An Activity that allows the user to change the application's settings.
 * 
 * @author Bartek Przybylski
 * 
 */
public class Preferences extends SherlockPreferenceActivity implements OnPreferenceChangeListener {
    
    private static final String TAG = "OwnCloudPreferences";
    private final int mNewSession = 47;
    private final int mEditSession = 48;
    private DbHandler mDbHandler;
    private Vector<OwnCloudSession> mSessions;
    private ListPreference mTrackingUpdateInterval;
    private CheckBoxPreference mDeviceTracking;
    private CheckBoxPreference pCode;
    private CheckBoxPreference pLogging;
    private Preference pLoggingHistory;
    private Preference pAboutApp;
    private int mSelectedMenuItem;


    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHandler = new DbHandler(getBaseContext());
        mSessions = new Vector<OwnCloudSession>();
        addPreferencesFromResource(R.xml.preferences);
        //populateAccountList();
        ActionBar actionBar = getSherlock().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Preference p = findPreference("manage_account");
        if (p != null)
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(getApplicationContext(), AccountSelectActivity.class);
                startActivity(i);
                return true;
            }
        });
        
        pCode = (CheckBoxPreference) findPreference("set_pincode");
        if (pCode != null){
            pCode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent i = new Intent(getApplicationContext(), PinCodeActivity.class);
                    i.putExtra(PinCodeActivity.EXTRA_ACTIVITY, "preferences");
                    i.putExtra(PinCodeActivity.EXTRA_NEW_STATE, newValue.toString());
                    startActivity(i);
                    return true;
                }
            });
            
           /* About App */
       pAboutApp = (Preference) findPreference("about_app");
       if (pAboutApp != null) { 
               pAboutApp.setTitle(String.format(getString(R.string.about_android), getString(R.string.app_name)));
               PackageInfo pkg;
               try {
                   pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
                   pAboutApp.setSummary(String.format(getString(R.string.about_version), pkg.versionName));
               } catch (NameNotFoundException e) {
                   Log_OC.e(TAG, "Error while showing about dialog", e);
               }
       }
       
       /* DISABLED FOR RELEASE UNTIL FIXED 
       pLogging = (CheckBoxPreference) findPreference("log_to_file");
       if (pLogging != null) {
           pLogging.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
               @Override
               public boolean onPreferenceChange(Preference preference, Object newValue) {
                   
                   String logpath = Environment.getExternalStorageDirectory()+File.separator+"owncloud"+File.separator+"log";
                
                   if(!pLogging.isChecked()) {
                       Log_OC.d("Debug", "start logging");
                       Log_OC.v("PATH", logpath);
                       Log_OC.startLogging(logpath);
                   }
                   else {
                       Log_OC.d("Debug", "stop logging");
                       Log_OC.stopLogging();
                   }
                   return true;
               }
           });
       }
       
       pLoggingHistory = (Preference) findPreference("log_history");
       if (pLoggingHistory != null) {
           pLoggingHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(),LogHistoryActivity.class);
                startActivity(intent);
                return true;
            }
        });
       }
       */
       
      }
    }

    @Override
    protected void onResume() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean state = appPrefs.getBoolean("set_pincode", false);
        pCode.setChecked(state);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        Intent intent;

        switch (item.getItemId()) {
        //case R.id.addSessionItem:
        case 1:
            intent = new Intent(this, PreferencesNewSession.class);
            startActivityForResult(intent, mNewSession);
            break;
        case R.id.SessionContextEdit:
            intent = new Intent(this, PreferencesNewSession.class);
            intent.putExtra("sessionId", mSessions.get(mSelectedMenuItem)
                    .getEntryId());
            intent.putExtra("sessionName", mSessions.get(mSelectedMenuItem)
                    .getName());
            intent.putExtra("sessionURL", mSessions.get(mSelectedMenuItem)
                    .getUrl());
            startActivityForResult(intent, mEditSession);
            break;
        case android.R.id.home:
            intent = new Intent(getBaseContext(), FileDisplayActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
        default:
            Log_OC.w(TAG, "Unknown menu item triggered");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        mDbHandler.close();
        super.onDestroy();
    }
    
    @Override
    /**
     * Updates various summaries after updates. Also starts and stops 
     * the
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Update current account summary
        /*if (preference.equals(mAccountList)) {
            mAccountList.setSummary(newValue.toString());
        }

        // Update tracking interval summary
        else*/ if (preference.equals(mTrackingUpdateInterval)) {
            String trackingSummary = getResources().getString(
                    R.string.prefs_trackmydevice_interval_summary);
            trackingSummary = String.format(trackingSummary,
                    newValue.toString());
            mTrackingUpdateInterval.setSummary(trackingSummary);
        }

        // Start or stop tracking service
        else if (preference.equals(mDeviceTracking)) {
            Intent locationServiceIntent = new Intent();
            locationServiceIntent
                    .setAction("com.owncloud.android.location.LocationLauncher");
            locationServiceIntent.putExtra("TRACKING_SETTING",
                    (Boolean) newValue);
            sendBroadcast(locationServiceIntent);
        } 
        return true;
    }
}
