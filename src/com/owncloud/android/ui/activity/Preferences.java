/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 Nextcloud
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;


/**
 * An Activity that allows the user to change the application's settings.
 *
 * It proxies the necessary calls via {@link android.support.v7.app.AppCompatDelegate} to be used
 * with AppCompat.
 */
public class Preferences extends PreferenceActivity {

    private static final String TAG = Preferences.class.getSimpleName();

    private static final int ACTION_SELECT_UPLOAD_PATH = 1;
    private static final int ACTION_SELECT_UPLOAD_VIDEO_PATH = 2;
    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;

    public static final String INSTANT_UPLOAD_PATH_ACCOUNT = "instant_upload_path_account";
    public static final String INSTANT_UPLOAD_PATH = "instant_upload_path";
    public static final String INSTANT_VIDEO_UPLOAD_PATH = "instant_video_upload_path";
    public static final String INSTANT_VIDEO_UPLOAD_PATH_ACCOUNT = "instant_video_upload_path_account";
    public static final String INSTANT_VIDEO_UPLOAD_PATH_USE_SUBFOLDERS = "instant_video_upload_path_use_subfolders";
    public static final String INSTANT_VIDEO_UPLOAD_ON_WIFI = "instant_video_upload_on_wifi";
    public static final String INSTANT_VIDEO_UPLOADING = "instant_video_uploading";
    public static final String PREFS_INSTANT_BEHAVIOUR = "prefs_instant_behaviour";
    public static final String ABOUT_APP = "about_app";
    public static final String INSTANT_UPLOADING_CATEGORY = "instant_uploading_category";
    public static final String INSTANT_UPLOAD_PATH_USE_SUBFOLDERS = "instant_upload_path_use_subfolders";
    public static final String INSTANT_UPLOAD_ON_WIFI = "instant_upload_on_wifi";
    public static final String INSTANT_UPLOADING = "instant_uploading";
    public static final String LOGGER = "logger";
    public static final String FEEDBACK = "feedback";
    public static final String RECOMMEND = "recommend";
    public static final String MORE = "more";
    public static final String HELP = "help";

    private CheckBoxPreference pCode;
    private Preference pAboutApp;
    private AppCompatDelegate mDelegate;

    private String mUploadPath;
    private String mUploadPathAccount;
    private PreferenceCategory mPrefInstantUploadCategory;
    private Preference mPrefInstantUpload;
    private Preference mPrefInstantUploadBehaviour;
    private Preference mPrefInstantUploadPath;
    private Preference mPrefInstantUploadUseSubfolders;
    private Preference mPrefInstantUploadPathWiFi;
    private Preference mPrefInstantVideoUpload;
    private Preference mPrefInstantVideoUploadPath;
    private Preference mPrefInstantVideoUploadUseSubfolders;
    private Preference mPrefInstantVideoUploadPathWiFi;
    private String mUploadVideoPath;
    private String mUploadVideoPathAccount;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.actionbar_settings);

        // For adding content description tag to a title field in the action bar
        int actionBarTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
        View actionBarTitleView = getWindow().getDecorView().findViewById(actionBarTitleId);
        if (actionBarTitleView != null) {    // it's null in Android 2.x
            getWindow().getDecorView().findViewById(actionBarTitleId).
                    setContentDescription(getString(R.string.actionbar_settings));
        }
        
        // Load package info
        String temp;
        try {
            PackageInfo pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
            temp = pkg.versionName;
        } catch (NameNotFoundException e) {
            temp = "";
            Log_OC.e(TAG, "Error while showing about dialog", e);
        } 
        final String appVersion = temp;
       
        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        pCode = (CheckBoxPreference) findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE);
        if (pCode != null){
            pCode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent i = new Intent(getApplicationContext(), PassCodeActivity.class);
                    Boolean incoming = (Boolean) newValue;

                    i.setAction(
                            incoming ? PassCodeActivity.ACTION_REQUEST_WITH_RESULT :
                                    PassCodeActivity.ACTION_CHECK_WITH_RESULT
                    );

                    startActivityForResult(i, incoming ? ACTION_REQUEST_PASSCODE :
                            ACTION_CONFIRM_PASSCODE);

                    // Don't update just yet, we will decide on it in onActivityResult
                    return false;
                }
            });
        }

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(MORE);
        
        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp =  findPreference(HELP);
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
       Preference pRecommend =  findPreference(RECOMMEND);
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

                        String recommendSubject =
                                String.format(getString(R.string.recommend_subject),
                                appName);
                        String recommendText = String.format(getString(R.string.recommend_text),
                                appName, downloadUrl);
                        
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
        Preference pFeedback =  findPreference(FEEDBACK);
        if (pFeedback != null){
            if (feedbackEnabled) {
                pFeedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String feedbackMail   =(String) getText(R.string.mail_feedback);
                        String feedback   =(String) getText(R.string.prefs_feedback) +
                                " - android v" + appVersion;
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

        boolean loggerEnabled = getResources().getBoolean(R.bool.logger_enabled) || BuildConfig.DEBUG;
        Preference pLogger =  findPreference(LOGGER);
        if (pLogger != null){
            if (loggerEnabled) {
                pLogger.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent loggerIntent = new Intent(getApplicationContext(), LogHistoryActivity.class);
                        startActivity(loggerIntent);

                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pLogger);
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

        mPrefInstantUploadPath =  findPreference(INSTANT_UPLOAD_PATH);
        if (mPrefInstantUploadPath != null){

            mPrefInstantUploadPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!mUploadPath.endsWith(OCFile.PATH_SEPARATOR)) {
                            mUploadPath += OCFile.PATH_SEPARATOR;
                        }
                        Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                        intent.putExtra(UploadPathActivity.KEY_INSTANT_UPLOAD_PATH, mUploadPath);
                        startActivityForResult(intent, ACTION_SELECT_UPLOAD_PATH);
                        return true;
                    }
                });
        }
        
        mPrefInstantUploadCategory =
                (PreferenceCategory) findPreference(INSTANT_UPLOADING_CATEGORY);

        mPrefInstantUploadUseSubfolders = findPreference(INSTANT_UPLOAD_PATH_USE_SUBFOLDERS);
        mPrefInstantUploadPathWiFi =  findPreference(INSTANT_UPLOAD_ON_WIFI);
        mPrefInstantUpload = findPreference(INSTANT_UPLOADING);
        
        toggleInstantPictureOptions(((CheckBoxPreference) mPrefInstantUpload).isChecked());
        
        mPrefInstantUpload.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                toggleInstantPictureOptions((Boolean) newValue);
                toggleInstantUploadBehaviour(
                        ((CheckBoxPreference)mPrefInstantVideoUpload).isChecked(),
                        (Boolean) newValue);
                return true;
            }
        });
       
        mPrefInstantVideoUploadPath =  findPreference(INSTANT_VIDEO_UPLOAD_PATH);
        if (mPrefInstantVideoUploadPath != null){

            mPrefInstantVideoUploadPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!mUploadVideoPath.endsWith(OCFile.PATH_SEPARATOR)) {
                            mUploadVideoPath += OCFile.PATH_SEPARATOR;
                        }
                        Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                        intent.putExtra(UploadPathActivity.KEY_INSTANT_UPLOAD_PATH,
                                mUploadVideoPath);
                        startActivityForResult(intent, ACTION_SELECT_UPLOAD_VIDEO_PATH);
                        return true;
                    }
                });
        }

        mPrefInstantVideoUploadUseSubfolders = findPreference(INSTANT_VIDEO_UPLOAD_PATH_USE_SUBFOLDERS);
        mPrefInstantVideoUploadPathWiFi =  findPreference(INSTANT_VIDEO_UPLOAD_ON_WIFI);
        mPrefInstantVideoUpload = findPreference(INSTANT_VIDEO_UPLOADING);
        toggleInstantVideoOptions(((CheckBoxPreference) mPrefInstantVideoUpload).isChecked());
        
        mPrefInstantVideoUpload.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                toggleInstantVideoOptions((Boolean) newValue);
                toggleInstantUploadBehaviour(
                        (Boolean) newValue,
                        ((CheckBoxPreference) mPrefInstantUpload).isChecked());
                return true;
            }
        });

        mPrefInstantUploadBehaviour = findPreference(PREFS_INSTANT_BEHAVIOUR);
        toggleInstantUploadBehaviour(
                ((CheckBoxPreference)mPrefInstantVideoUpload).isChecked(),
                ((CheckBoxPreference)mPrefInstantUpload).isChecked());

        /* About App */
       pAboutApp = (Preference) findPreference(ABOUT_APP);
       if (pAboutApp != null) { 
               pAboutApp.setTitle(String.format(getString(R.string.about_android),
                       getString(R.string.app_name)));
               pAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
       }

       loadInstantUploadPath();
       loadInstantUploadVideoPath();
    }
    
    private void toggleInstantPictureOptions(Boolean value){
        if (value){
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadPathWiFi);
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadPath);
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadUseSubfolders);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadPathWiFi);
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadPath);
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadUseSubfolders);
        }
    }
    
    private void toggleInstantVideoOptions(Boolean value){
        if (value){
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadPathWiFi);
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadPath);
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadUseSubfolders);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadPathWiFi);
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadPath);
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadUseSubfolders);
        }
    }

    private void toggleInstantUploadBehaviour(Boolean video, Boolean picture){
        if (picture || video){
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadBehaviour);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadBehaviour);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean state = appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
        pCode.setChecked(state);
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

        if (requestCode == ACTION_SELECT_UPLOAD_PATH && resultCode == RESULT_OK){

            OCFile folderToUpload =
                    (OCFile) data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);

            mUploadPath = folderToUpload.getRemotePath();

            mUploadPath = DisplayUtils.getPathWithoutLastSlash(mUploadPath);

            mUploadPathAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()).name;

            // Show the path on summary preference
            mPrefInstantUploadPath.setSummary(getUploadAccountPath(mUploadPathAccount, mUploadPath));

            saveInstantUploadPathOnPreferences();

        } else if (requestCode == ACTION_SELECT_UPLOAD_VIDEO_PATH && resultCode == RESULT_OK){

            OCFile folderToUploadVideo =
                    (OCFile) data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);

            mUploadVideoPath = folderToUploadVideo.getRemotePath();

            mUploadVideoPath = DisplayUtils.getPathWithoutLastSlash(mUploadVideoPath);

            mUploadVideoPathAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()).name;

            // Show the video path on summary preference
            mPrefInstantVideoUploadPath.setSummary(getUploadAccountPath(mUploadVideoPathAccount, mUploadVideoPath));

            saveInstantUploadVideoPathOnPreferences();
        } else if (requestCode == ACTION_REQUEST_PASSCODE && resultCode == RESULT_OK) {
            String passcode = data.getStringExtra(PassCodeActivity.KEY_PASSCODE);
            if (passcode != null && passcode.length() == 4) {
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();

                for (int i = 1; i <= 4; ++i) {
                    appPrefs.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i, passcode.substring(i-1, i));
                }
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true);
                appPrefs.commit();
                Toast.makeText(this, R.string.pass_code_stored, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {

                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
                appPrefs.commit();

                Toast.makeText(this, R.string.pass_code_removed, Toast.LENGTH_LONG).show();
            }
        }
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }
    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    /**
     * Returns a combined string of accountName and uploadPath to be displayed to user.
     */
    private String getUploadAccountPath(String accountName, String uploadPath) {
        return accountName + ":" + uploadPath;
    }

    /**
     * Load upload path set on preferences
     */
    private void loadInstantUploadPath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadPath = appPrefs.getString(INSTANT_UPLOAD_PATH, getString(R.string.instant_upload_path));
        mUploadPathAccount = appPrefs.getString(INSTANT_UPLOAD_PATH_ACCOUNT,
                AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()).name);
        mPrefInstantUploadPath.setSummary(getUploadAccountPath(mUploadPathAccount, mUploadPath));
    }

    /**
     * Save the "Instant Upload Path" and corresponding account on preferences
     */
    private void saveInstantUploadPathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(INSTANT_UPLOAD_PATH, mUploadPath)
                .putString(INSTANT_UPLOAD_PATH_ACCOUNT, mUploadPathAccount);
        editor.commit();
    }

    /**
     * Load upload video path set on preferences
     */
    private void loadInstantUploadVideoPath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadVideoPath = appPrefs.getString(INSTANT_VIDEO_UPLOAD_PATH, getString(R.string.instant_upload_path));
        mUploadVideoPathAccount = appPrefs.getString(INSTANT_VIDEO_UPLOAD_PATH_ACCOUNT,
                AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()).name);
        mPrefInstantVideoUploadPath.setSummary(getUploadAccountPath(mUploadVideoPathAccount, mUploadVideoPath));
    }

    /**
     * Save the "Instant Video Upload Path" and corresponding account on preferences
     */
    private void saveInstantUploadVideoPathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(INSTANT_VIDEO_UPLOAD_PATH, mUploadVideoPath)
                .putString(INSTANT_VIDEO_UPLOAD_PATH_ACCOUNT, mUploadVideoPathAccount);
        editor.commit();
    }
}
