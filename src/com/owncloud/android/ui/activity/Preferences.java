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
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.RadioButtonPreference;
import com.owncloud.android.utils.DisplayUtils;


/**
 * An Activity that allows the user to change the application's settings.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class Preferences extends SherlockPreferenceActivity implements AccountManagerCallback<Boolean> {
    
    private static final String TAG = "OwnCloudPreferences";

    private static final int ACTION_SELECT_UPLOAD_PATH = 1;
    private static final int ACTION_SELECT_UPLOAD_VIDEO_PATH = 2;

    private DbHandler mDbHandler;
    private CheckBoxPreference pCode;
    private Preference pAboutApp;

    private PreferenceCategory mAccountsPrefCategory = null;
    private final Handler mHandler = new Handler();
    private String mAccountName;
    private boolean mShowContextMenu = false;
    private String mUploadPath;
    private PreferenceCategory mPrefInstantUploadCategory;
    private Preference mPrefInstantUpload;
    private Preference mPrefInstantUploadPath;
    private Preference mPrefInstantUploadPathWiFi;
    private Preference mPrefInstantVideoUpload;
    private Preference mPrefInstantVideoUploadPath;
    private Preference mPrefInstantVideoUploadPathWiFi;
    private String mUploadVideoPath;


    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHandler = new DbHandler(getBaseContext());
        addPreferencesFromResource(R.xml.preferences);

        ActionBar actionBar = getSherlock().getActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.actionbar_settings);

        // Load the accounts category for adding the list of accounts
        mAccountsPrefCategory = (PreferenceCategory) findPreference("accounts_category");

        ListView listView = getListView();
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                ListAdapter listAdapter = listView.getAdapter();
                Object obj = listAdapter.getItem(position);

                if (obj != null && obj instanceof RadioButtonPreference) {
                    mShowContextMenu = true;
                    mAccountName = ((RadioButtonPreference) obj).getKey();

                    Preferences.this.openContextMenu(listView);

                    View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
                    return longListener.onLongClick(view);
                }
                return false;
            }
        });
        
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
                        String feedback   =(String) getText(R.string.prefs_feedback) + " - android v" + appVersion;
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

        mPrefInstantUploadPath =  findPreference("instant_upload_path");
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
        
        mPrefInstantUploadCategory = (PreferenceCategory) findPreference("instant_uploading_category");
        
        mPrefInstantUploadPathWiFi =  findPreference("instant_upload_on_wifi");
        mPrefInstantUpload = findPreference("instant_uploading");
        
        toggleInstantPictureOptions(((CheckBoxPreference) mPrefInstantUpload).isChecked());
        
        mPrefInstantUpload.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                toggleInstantPictureOptions((Boolean) newValue);
                return true;
            }
        });
       
        mPrefInstantVideoUploadPath =  findPreference("instant_video_upload_path");
        if (mPrefInstantVideoUploadPath != null){

            mPrefInstantVideoUploadPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!mUploadVideoPath.endsWith(OCFile.PATH_SEPARATOR)) {
                            mUploadVideoPath += OCFile.PATH_SEPARATOR;
                        }
                        Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                        intent.putExtra(UploadPathActivity.KEY_INSTANT_UPLOAD_PATH, mUploadVideoPath);
                        startActivityForResult(intent, ACTION_SELECT_UPLOAD_VIDEO_PATH);
                        return true;
                    }
                });
        }
        
        mPrefInstantVideoUploadPathWiFi =  findPreference("instant_video_upload_on_wifi");
        mPrefInstantVideoUpload = findPreference("instant_video_uploading");
        toggleInstantVideoOptions(((CheckBoxPreference) mPrefInstantVideoUpload).isChecked());
        
        mPrefInstantVideoUpload.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                toggleInstantVideoOptions((Boolean) newValue);
                return true;
            }
        });
            
        /* About App */
       pAboutApp = (Preference) findPreference("about_app");
       if (pAboutApp != null) { 
               pAboutApp.setTitle(String.format(getString(R.string.about_android), getString(R.string.app_name)));
               pAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
       }

       loadInstantUploadPath();
       loadInstantUploadVideoPath();

    }
    
    private void toggleInstantPictureOptions(Boolean value){
        if (value){
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadPathWiFi);
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadPath);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadPathWiFi);
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadPath);
        }
    }
    
    private void toggleInstantVideoOptions(Boolean value){
        if (value){
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadPathWiFi);
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadPath);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadPathWiFi);
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadPath);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        // Filter for only showing contextual menu when long press on the
        // accounts
        if (mShowContextMenu) {
            getMenuInflater().inflate(R.menu.account_picker_long_click, menu);
            mShowContextMenu = false;
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     * Called when the user clicked on an item into the context menu created at
     * {@link #onCreateContextMenu(ContextMenu, View, ContextMenuInfo)} for
     * every ownCloud {@link Account} , containing 'secondary actions' for them.
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account accounts[] = am.getAccountsByType(MainApp.getAccountType());
        for (Account a : accounts) {
            if (a.name.equals(mAccountName)) {
                if (item.getItemId() == R.id.change_password) {

                    // Change account password
                    Intent updateAccountCredentials = new Intent(this, AuthenticatorActivity.class);
                    updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, a);
                    updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACTION,
                            AuthenticatorActivity.ACTION_UPDATE_TOKEN);
                    startActivity(updateAccountCredentials);

                } else if (item.getItemId() == R.id.delete_account) {

                    // Remove account
                    am.removeAccount(a, this, mHandler);
                }
            }
        }

        return true;
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            Account a = AccountUtils.getCurrentOwnCloudAccount(this);
            String accountName = "";
            if (a == null) {
                Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
                if (accounts.length != 0)
                    accountName = accounts[0].name;
                AccountUtils.setCurrentOwnCloudAccount(this, accountName);
            }
            addAccountsCheckboxPreferences();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean state = appPrefs.getBoolean("set_pincode", false);
        pCode.setChecked(state);

        // Populate the accounts category with the list of accounts
        addAccountsCheckboxPreferences();
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

            OCFile folderToUpload = (OCFile) data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);

            mUploadPath = folderToUpload.getRemotePath();

            mUploadPath = DisplayUtils.getPathWithoutLastSlash(mUploadPath);

            // Show the path on summary preference
            mPrefInstantUploadPath.setSummary(mUploadPath);

            saveInstantUploadPathOnPreferences();

        } else if (requestCode == ACTION_SELECT_UPLOAD_VIDEO_PATH && resultCode == RESULT_OK){

            OCFile folderToUploadVideo = (OCFile) data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);

            mUploadVideoPath = folderToUploadVideo.getRemotePath();

            mUploadVideoPath = DisplayUtils.getPathWithoutLastSlash(mUploadVideoPath);

            // Show the video path on summary preference
            mPrefInstantVideoUploadPath.setSummary(mUploadVideoPath);

            saveInstantUploadVideoPathOnPreferences();
        }
    }

    @Override
    protected void onDestroy() {
        mDbHandler.close();
        super.onDestroy();
    }

    /**
     * Create the list of accounts that has been added into the app
     */
    @SuppressWarnings("deprecation")
    private void addAccountsCheckboxPreferences() {

        // Remove accounts in case list is refreshing for avoiding to have
        // duplicate items
        if (mAccountsPrefCategory.getPreferenceCount() > 0) {
            mAccountsPrefCategory.removeAll();
        }

        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account accounts[] = am.getAccountsByType(MainApp.getAccountType());
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());

        if (am.getAccountsByType(MainApp.getAccountType()).length == 0) {
            // Show create account screen if there isn't any account
            am.addAccount(MainApp.getAccountType(), null, null, null, this,
                    null,
                    null);
        }
        else {

            for (Account a : accounts) {
                RadioButtonPreference accountPreference = new RadioButtonPreference(this);
                accountPreference.setKey(a.name);
                // Handle internationalized domain names
                accountPreference.setTitle(DisplayUtils.convertIdn(a.name, false));
                mAccountsPrefCategory.addPreference(accountPreference);

                // Check the current account that is being used
                if (a.name.equals(currentAccount.name)) {
                    accountPreference.setChecked(true);
                } else {
                    accountPreference.setChecked(false);
                }

                accountPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String key = preference.getKey();
                        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                        Account accounts[] = am.getAccountsByType(MainApp.getAccountType());
                        for (Account a : accounts) {
                            RadioButtonPreference p = (RadioButtonPreference) findPreference(a.name);
                            if (key.equals(a.name)) {
                                boolean accountChanged = !p.isChecked(); 
                                p.setChecked(true);
                                AccountUtils.setCurrentOwnCloudAccount(
                                        getApplicationContext(),
                                        a.name
                                );
                                if (accountChanged) {
                                    // restart the main activity
                                    Intent i = new Intent(
                                            Preferences.this, 
                                            FileDisplayActivity.class
                                    );
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                } else {
                                    finish();
                                }
                            } else {
                                p.setChecked(false);
                            }
                        }
                        return (Boolean) newValue;
                    }
                });

            }

            // Add Create Account preference at the end of account list if
            // Multiaccount is enabled
            if (getResources().getBoolean(R.bool.multiaccount_support)) {
                createAddAccountPreference();
            }

        }
    }

    /**
     * Create the preference for allow adding new accounts
     */
    private void createAddAccountPreference() {
        Preference addAccountPref = new Preference(this);
        addAccountPref.setKey("add_account");
        addAccountPref.setTitle(getString(R.string.prefs_add_account));
        mAccountsPrefCategory.addPreference(addAccountPref);

        addAccountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AccountManager am = AccountManager.get(getApplicationContext());
                am.addAccount(MainApp.getAccountType(), null, null, null, Preferences.this, null, null);
                return true;
            }
        });

    }

    /**
     * Load upload path set on preferences
     */
    private void loadInstantUploadPath() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadPath = appPrefs.getString("instant_upload_path", getString(R.string.instant_upload_path));
        mPrefInstantUploadPath.setSummary(mUploadPath);
    }

    /**
     * Save the "Instant Upload Path" on preferences
     */
    private void saveInstantUploadPathOnPreferences() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());        
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("instant_upload_path", mUploadPath);
        editor.commit();
    }

    /**
     * Load upload video path set on preferences
     */
    private void loadInstantUploadVideoPath() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadVideoPath = appPrefs.getString("instant_video_upload_path", getString(R.string.instant_upload_path));
        mPrefInstantVideoUploadPath.setSummary(mUploadVideoPath);
    }

    /**
     * Save the "Instant Video Upload Path" on preferences
     */
    private void saveInstantUploadVideoPathOnPreferences() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());        
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("instant_video_upload_path", mUploadVideoPath);
        editor.commit();
    }
}
