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

import android.accounts.Account;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Log_OC;


/**
 * An Activity that allows the user to change the application's settings.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class Preferences extends SherlockPreferenceActivity {
    
    private static final String TAG = "OwnCloudPreferences";
    private DbHandler mDbHandler;
    private CheckBoxPreference pCode;
    //private CheckBoxPreference pLogging;
    //private Preference pLoggingHistory;
    private Preference pAboutApp;


    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHandler = new DbHandler(getBaseContext());
        addPreferencesFromResource(R.xml.preferences);
        //populateAccountList();
        ActionBar actionBar = getSherlock().getActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());
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
            
        }
        
        

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("more");
        
        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp =  findPreference("help");
        if (pHelp != null ){
            if (helpEnabled) {
                pHelp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String helpWeb   =(String) getText(R.string.url_help);
                        if (helpWeb != null && helpWeb.length() > 0) {
                            Uri uriUrl = Uri.parse(helpWeb);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                            startActivity(intent);
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pHelp);
            }
            
        }
        
       
       boolean recommendEnabled = getResources().getBoolean(R.bool.recommend_enabled);
       Preference pRecommend =  findPreference("recommend");
        if (pRecommend != null){
            if (recommendEnabled) {
                pRecommend.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        Intent intent = new Intent(Intent.ACTION_SENDTO); 
                        intent.setType("text/plain");
                        intent.setData(Uri.parse(getString(R.string.mail_recommend))); 
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                        
                        String appName = getString(R.string.app_name);
                        String downloadUrl = getString(R.string.url_app_download);
                        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(Preferences.this);
                        String username = currentAccount.name.substring(0, currentAccount.name.lastIndexOf('@'));
                        
                        String recommendSubject = String.format(getString(R.string.recommend_subject), appName);
                        String recommendText = String.format(getString(R.string.recommend_text), appName, downloadUrl, username);
                        
                        intent.putExtra(Intent.EXTRA_SUBJECT, recommendSubject);
                        intent.putExtra(Intent.EXTRA_TEXT, recommendText);
                        startActivity(intent);


                        return(true);

                    }
                });
            } else {
                preferenceCategory.removePreference(pRecommend);
            }
            
        }
        
        boolean feedbackEnabled = getResources().getBoolean(R.bool.feedback_enabled);
        Preference pFeedback =  findPreference("feedback");
        if (pFeedback != null){
            if (feedbackEnabled) {
                pFeedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String feedbackMail   =(String) getText(R.string.mail_feedback);
                        String feedback   =(String) getText(R.string.prefs_feedback);
                        Intent intent = new Intent(Intent.ACTION_SENDTO); 
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, feedback);
                        
                        intent.setData(Uri.parse(feedbackMail)); 
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                        startActivity(intent);
                        
                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pFeedback);
            }
            
        }
        
        boolean imprintEnabled = getResources().getBoolean(R.bool.imprint_enabled);
        Preference pImprint =  findPreference("imprint");
        if (pImprint != null) {
            if (imprintEnabled) {
                pImprint.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String imprintWeb = (String) getText(R.string.url_imprint);
                        if (imprintWeb != null && imprintWeb.length() > 0) {
                            Uri uriUrl = Uri.parse(imprintWeb);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                            startActivity(intent);
                        }
                        //ImprintDialog.newInstance(true).show(preference.get, "IMPRINT_DIALOG");
                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pImprint);
            }
        }
            
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
    
}
