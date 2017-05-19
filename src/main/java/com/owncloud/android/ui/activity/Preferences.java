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

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
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
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ExternalLinksProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datastorage.DataStorageProvider;
import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;

import java.io.IOException;

/**
 * An Activity that allows the user to change the application's settings.
 *
 * It proxies the necessary calls via {@link android.support.v7.app.AppCompatDelegate} to be used with AppCompat.
 */
public class Preferences extends PreferenceActivity
        implements StorageMigration.StorageMigrationProgressListener {
    
    private static final String TAG = Preferences.class.getSimpleName();

    private static final String SCREEN_NAME = "Settings";

    private static final int ACTION_SELECT_UPLOAD_PATH = 1;
    private static final int ACTION_SELECT_UPLOAD_VIDEO_PATH = 2;
    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;

    private static final int ACTION_REQUEST_CODE_DAVDROID_SETUP = 10;

    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI";

    /**
     * The user's server base uri.
     */
    private Uri mUri;

    private SwitchPreference pCode;
    private SwitchPreference fPrint;
    private SwitchPreference mShowHiddenFiles;
    private Preference pAboutApp;
    private AppCompatDelegate mDelegate;

    private String mUploadPath;
    private PreferenceCategory mPrefInstantUploadCategory;
    private Preference mPrefInstantUpload;
    private Preference mPrefInstantUploadBehaviour;
    private Preference mPrefInstantUploadPath;
    private Preference mPrefInstantUploadUseSubfolders;
    private Preference mPrefInstantPictureUploadOnlyOnCharging;
    private Preference mPrefInstantUploadPathWiFi;
    private Preference mPrefInstantVideoUpload;
    private Preference mPrefInstantVideoUploadPath;
    private Preference mPrefInstantVideoUploadUseSubfolders;
    private Preference mPrefInstantVideoUploadPathWiFi;
    private Preference mPrefInstantVideoUploadOnlyOnCharging;
    private String mUploadVideoPath;
    private ListPreference mPrefStoragePath;
    private String mStoragePath;

    public static class PreferenceKeys {
        public static final String STORAGE_PATH = "storage_path";
        public static final String INSTANT_UPLOAD_PATH = "instant_upload_path";
        public static final String INSTANT_VIDEO_UPLOAD_PATH = "instant_video_upload_path";
    }

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

        // retrieve user's base uri
        setupBaseUri();

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

        // Synced folders
        PreferenceCategory preferenceCategoryFolderSync = (PreferenceCategory) findPreference("folder_sync");
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preference_screen");

        if (!getResources().getBoolean(R.bool.syncedFolder_light)) {
            preferenceScreen.removePreference(preferenceCategoryFolderSync);
        } else {
            // Upload on WiFi
            final ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
            final Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());

            final SwitchPreference pUploadOnWifiCheckbox = (SwitchPreference) findPreference("synced_folder_on_wifi");
            pUploadOnWifiCheckbox.setChecked(
                    arbitraryDataProvider.getBooleanValue(account, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI));

            pUploadOnWifiCheckbox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    arbitraryDataProvider.storeOrUpdateKeyValue(account, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI,
                            String.valueOf(pUploadOnWifiCheckbox.isChecked()));

                    return true;
                }
            });

            Preference pSyncedFolder = findPreference("folder_sync_folders");
            if (pSyncedFolder != null) {
                if (getResources().getBoolean(R.bool.syncedFolder_light)
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pSyncedFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent folderSyncIntent = new Intent(getApplicationContext(), FolderSyncActivity.class);
                            folderSyncIntent.putExtra(FolderSyncActivity.EXTRA_SHOW_SIDEBAR, false);
                            startActivity(folderSyncIntent);
                            return true;
                        }
                    });
                } else {
                    preferenceCategoryFolderSync.removePreference(pSyncedFolder);
                }
            }
        }

        PreferenceCategory preferenceCategoryDetails = (PreferenceCategory) findPreference("details");

        boolean fPassCodeEnabled = getResources().getBoolean(R.bool.passcode_enabled);
        pCode = (SwitchPreference) findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE);
        if (pCode != null && fPassCodeEnabled) {
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
        } else {
            preferenceCategoryDetails.removePreference(pCode);
        }

        boolean fPrintEnabled = getResources().getBoolean(R.bool.fingerprint_enabled);
        fPrint = (SwitchPreference) findPreference(FingerprintActivity.PREFERENCE_USE_FINGERPRINT);
        if (fPrint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (FingerprintActivity.isFingerprintCapable(MainApp.getAppContext()) && fPrintEnabled) {
                    fPrint.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            Boolean incoming = (Boolean) newValue;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (FingerprintActivity.isFingerprintReady(MainApp.getAppContext())) {
                                    SharedPreferences appPrefs =
                                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    SharedPreferences.Editor editor = appPrefs.edit();
                                    editor.putBoolean("use_fingerprint", incoming);
                                    editor.apply();
                                    return true;
                                } else {
                                    if (incoming) {
                                        Toast.makeText(
                                                MainApp.getAppContext(),
                                                R.string.prefs_fingerprint_notsetup,
                                                Toast.LENGTH_LONG)
                                                .show();
                                        fPrint.setChecked(false);
                                    }
                                    SharedPreferences appPrefs =
                                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    SharedPreferences.Editor editor = appPrefs.edit();
                                    editor.putBoolean("use_fingerprint", false);
                                    editor.apply();
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        }
                    });
                    if (!FingerprintActivity.isFingerprintReady(MainApp.getAppContext())) {
                        fPrint.setChecked(false);
                    }

                } else {
                    preferenceCategoryDetails.removePreference(fPrint);
                }
            } else {
                preferenceCategoryDetails.removePreference(fPrint);
            }
        }

        boolean fShowHiddenFilesEnabled = getResources().getBoolean(R.bool.passcode_enabled);
        mShowHiddenFiles = (SwitchPreference) findPreference("show_hidden_files");

        if (fShowHiddenFilesEnabled) {
            mShowHiddenFiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences appPrefs =
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = appPrefs.edit();
                    editor.putBoolean("show_hidden_files_pref", mShowHiddenFiles.isChecked());
                    editor.apply();
                    return true;
                }
            });
        } else {
            preferenceCategoryDetails.removePreference(mShowHiddenFiles);

        }

        PreferenceCategory preferenceCategoryMore = (PreferenceCategory) findPreference("more");

        boolean calendarContactsEnabled = getResources().getBoolean(R.bool.davdroid_integration_enabled);
        Preference pCalendarContacts = findPreference("calendar_contacts");
        if (pCalendarContacts != null) {
            if (calendarContactsEnabled) {
                pCalendarContacts.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            launchDavDroidLogin();
                        } catch (Throwable t) {
                            Log_OC.e(TAG, "Base Uri for account could not be resolved to call DAVdroid!", t);
                            Toast.makeText(
                                    MainApp.getAppContext(),
                                    R.string.prefs_calendar_contacts_address_resolve_error,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategoryMore.removePreference(pCalendarContacts);
            }
        }

        boolean contactsBackupEnabled = !getResources().getBoolean(R.bool.show_drawer_contacts_backup)
                && getResources().getBoolean(R.bool.contacts_backup);
        Preference pContactsBackup = findPreference("contacts");
        if (pCalendarContacts != null) {
            if (contactsBackupEnabled) {
                pContactsBackup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent contactsIntent = new Intent(getApplicationContext(), ContactsPreferenceActivity.class);
                        startActivity(contactsIntent);
                        return true;
                    }
                });
            } else {
                preferenceCategoryMore.removePreference(pContactsBackup);
            }
        }

        if (!fShowHiddenFilesEnabled && !fPrintEnabled && !fPassCodeEnabled) {
            preferenceScreen.removePreference(preferenceCategoryDetails);
        }

        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp = findPreference("help");
        if (pHelp != null) {
            if (helpEnabled) {
                pHelp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String helpWeb = getString(R.string.url_help);
                        if (helpWeb != null && helpWeb.length() > 0) {
                            Uri uriUrl = Uri.parse(helpWeb);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                            startActivity(intent);
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategoryMore.removePreference(pHelp);
            }
        }

        boolean recommendEnabled = getResources().getBoolean(R.bool.recommend_enabled);
        Preference pRecommend = findPreference("recommend");
        if (pRecommend != null) {
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

                        String recommendSubject = String.format(getString(R.string.recommend_subject), appName);
                        String recommendText = String.format(getString(R.string.recommend_text), appName, downloadUrl);

                        intent.putExtra(Intent.EXTRA_SUBJECT, recommendSubject);
                        intent.putExtra(Intent.EXTRA_TEXT, recommendText);
                        startActivity(intent);

                        return true;

                    }
                });
            } else {
                preferenceCategoryMore.removePreference(pRecommend);
            }
        }

        boolean feedbackEnabled = getResources().getBoolean(R.bool.feedback_enabled);
        Preference pFeedback = findPreference("feedback");
        if (pFeedback != null) {
            if (feedbackEnabled) {
                pFeedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String feedbackMail = getString(R.string.mail_feedback);
                        String feedback = getText(R.string.prefs_feedback) + " - android v" + appVersion;
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
                preferenceCategoryMore.removePreference(pFeedback);
            }
        }

        boolean loggerEnabled = getResources().getBoolean(R.bool.logger_enabled) || BuildConfig.DEBUG;
        Preference pLogger = findPreference("logger");
        if (pLogger != null) {
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
                preferenceCategoryMore.removePreference(pLogger);
            }
        }

        boolean imprintEnabled = getResources().getBoolean(R.bool.imprint_enabled);
        Preference pImprint = findPreference("imprint");
        if (pImprint != null) {
            if (imprintEnabled) {
                pImprint.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String imprintWeb = getString(R.string.url_imprint);
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
                preferenceCategoryMore.removePreference(pImprint);
            }
        }

        mPrefStoragePath = (ListPreference) findPreference(PreferenceKeys.STORAGE_PATH);
        if (mPrefStoragePath != null) {
            StoragePoint[] storageOptions = DataStorageProvider.getInstance().getAvailableStoragePoints();
            String[] entries = new String[storageOptions.length];
            String[] values = new String[storageOptions.length];
            for (int i = 0; i < storageOptions.length; ++i) {
                entries[i] = storageOptions[i].getDescription();
                values[i] = storageOptions[i].getPath();
            }
            mPrefStoragePath.setEntries(entries);
            mPrefStoragePath.setEntryValues(values);

            mPrefStoragePath.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String newPath = (String) newValue;
                        if (mStoragePath.equals(newPath)) {
                            return true;
                        }

                    StorageMigration storageMigration = new StorageMigration(Preferences.this, mStoragePath, newPath);

                    storageMigration.setStorageMigrationProgressListener(Preferences.this);

                    storageMigration.migrate();

                    return false;
                }
            });
        }

        mPrefInstantUploadCategory = (PreferenceCategory) findPreference("instant_uploading_category");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Instant upload via preferences on pre Android Marshmallow
            mPrefInstantUploadPath = findPreference("instant_upload_path");
            if (mPrefInstantUploadPath != null) {

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

            mPrefInstantUploadUseSubfolders = findPreference("instant_upload_path_use_subfolders");
            mPrefInstantUploadPathWiFi = findPreference("instant_upload_on_wifi");
            mPrefInstantPictureUploadOnlyOnCharging = findPreference("instant_upload_on_charging");
            mPrefInstantUpload = findPreference("instant_uploading");

            toggleInstantPictureOptions(((SwitchPreference) mPrefInstantUpload).isChecked());

            mPrefInstantUpload.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    toggleInstantPictureOptions((Boolean) newValue);
                    toggleInstantUploadBehaviour(
                            ((SwitchPreference) mPrefInstantVideoUpload).isChecked(),
                            (Boolean) newValue);
                    return true;
                }
            });

        mPrefInstantVideoUploadPath = findPreference(PreferenceKeys.INSTANT_VIDEO_UPLOAD_PATH);
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

            mPrefInstantVideoUploadUseSubfolders = findPreference("instant_video_upload_path_use_subfolders");
            mPrefInstantVideoUploadPathWiFi = findPreference("instant_video_upload_on_wifi");
            mPrefInstantVideoUpload = findPreference("instant_video_uploading");
            mPrefInstantVideoUploadOnlyOnCharging = findPreference("instant_video_upload_on_charging");
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

            mPrefInstantUploadBehaviour = findPreference("prefs_instant_behaviour");
            toggleInstantUploadBehaviour(
                    ((CheckBoxPreference) mPrefInstantVideoUpload).isChecked(),
                    ((CheckBoxPreference) mPrefInstantUpload).isChecked());

            loadInstantUploadPath();
            loadInstantUploadVideoPath();
        } else {
            // Instant upload is handled via synced folders on Android Lollipop and up
            getPreferenceScreen().removePreference(mPrefInstantUploadCategory);
        }

        // About category
        PreferenceCategory preferenceCategoryAbout = (PreferenceCategory) findPreference("about");

        /* About App */
        pAboutApp = findPreference("about_app");
        if (pAboutApp != null) {
            pAboutApp.setTitle(String.format(getString(R.string.about_android), getString(R.string.app_name)));
            pAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
        }

        // privacy
        boolean privacyEnabled = getResources().getBoolean(R.bool.privacy_enabled);
        Preference privacyPreference = findPreference("privacy");
        if (privacyPreference != null) {
            if (privacyEnabled) {
                privacyPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String privacyUrl = getString(R.string.privacy_url);
                        if (privacyUrl.length() > 0) {
                            Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE,
                                    getResources().getString(R.string.privacy));
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, privacyUrl);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1);
                            startActivity(externalWebViewIntent);
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategoryAbout.removePreference(privacyPreference);
            }
        }

        loadExternalSettingLinks(preferenceCategoryMore);

        loadStoragePath();
    }

    private void launchDavDroidLogin()
            throws com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException,
            OperationCanceledException,
            AuthenticatorException,
            IOException {
        Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());

        Intent davDroidLoginIntent = new Intent();
        davDroidLoginIntent.setClassName("at.bitfire.davdroid", "at.bitfire.davdroid.ui.setup.LoginActivity");
        if (getPackageManager().resolveActivity(davDroidLoginIntent, 0) != null) {
            // arguments
            if (mUri != null) {
                davDroidLoginIntent.putExtra("url", mUri.toString() + AccountUtils.DAV_PATH);
            }
            davDroidLoginIntent.putExtra("username", AccountUtils.getAccountUsername(account.name));
            //loginIntent.putExtra("password", "...");
            startActivityForResult(davDroidLoginIntent, ACTION_REQUEST_CODE_DAVDROID_SETUP);
        } else {
            // DAVdroid not installed
            Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=at.bitfire.davdroid"));

            // launch market(s)
            if (installIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(installIntent);
            } else {
                // no f-droid market app or Play store installed --> launch browser for f-droid url
                Intent downloadIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://f-droid.org/repository/browse/?fdid=at.bitfire.davdroid"));
                startActivity(downloadIntent);

                Toast.makeText(
                        MainApp.getAppContext(),
                        R.string.prefs_calendar_contacts_no_store_error,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void setupBaseUri() {
        // retrieve and set user's base URI
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
                    OwnCloudAccount ocAccount = new OwnCloudAccount(account, MainApp.getAppContext());
                    mUri = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, getApplicationContext()).getBaseUri();
                } catch (Throwable t) {
                    Log_OC.e(TAG,"Error retrieving user's base URI", t);
                }
            }
        });
        t.start();
    }
    
    private void toggleInstantPictureOptions(Boolean value){
        if (value) {
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadPathWiFi);
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadPath);
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadUseSubfolders);
            mPrefInstantUploadCategory.addPreference(mPrefInstantPictureUploadOnlyOnCharging);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadPathWiFi);
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadPath);
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadUseSubfolders);
            mPrefInstantUploadCategory.removePreference(mPrefInstantPictureUploadOnlyOnCharging);
        }
    }

    private void toggleInstantVideoOptions(Boolean value){
        if (value) {
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadPathWiFi);
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadPath);
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadUseSubfolders);
            mPrefInstantUploadCategory.addPreference(mPrefInstantVideoUploadOnlyOnCharging);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadPathWiFi);
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadPath);
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadUseSubfolders);
            mPrefInstantUploadCategory.removePreference(mPrefInstantVideoUploadOnlyOnCharging);
        }
    }

    private void toggleInstantUploadBehaviour(Boolean video, Boolean picture){
        if (picture || video) {
            mPrefInstantUploadCategory.addPreference(mPrefInstantUploadBehaviour);
        } else {
            mPrefInstantUploadCategory.removePreference(mPrefInstantUploadBehaviour);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);

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

        if (requestCode == ACTION_SELECT_UPLOAD_PATH && resultCode == RESULT_OK) {

            OCFile folderToUpload =  data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);

            mUploadPath = folderToUpload.getRemotePath();

            mUploadPath = DisplayUtils.getPathWithoutLastSlash(mUploadPath);

            // Show the path on summary preference
            mPrefInstantUploadPath.setSummary(mUploadPath);

            saveInstantUploadPathOnPreferences();

        } else if (requestCode == ACTION_SELECT_UPLOAD_VIDEO_PATH && resultCode == RESULT_OK) {

            OCFile folderToUploadVideo = data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);

            mUploadVideoPath = folderToUploadVideo.getRemotePath();

            mUploadVideoPath = DisplayUtils.getPathWithoutLastSlash(mUploadVideoPath);

            // Show the video path on summary preference
            mPrefInstantVideoUploadPath.setSummary(mUploadVideoPath);

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
                appPrefs.apply();
                Toast.makeText(this, R.string.pass_code_stored, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {

                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
                appPrefs.apply();

                Toast.makeText(this, R.string.pass_code_removed, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ACTION_REQUEST_CODE_DAVDROID_SETUP && resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.prefs_calendar_contacts_sync_setup_successful, Toast.LENGTH_LONG).show();
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

    private void loadExternalSettingLinks(PreferenceCategory preferenceCategory) {
        if (getBaseContext().getResources().getBoolean(R.bool.show_external_links)) {
            ExternalLinksProvider externalLinksProvider = new ExternalLinksProvider(getContentResolver());

            for (final ExternalLink link : externalLinksProvider.getExternalLink(ExternalLinkType.SETTINGS)) {

                // only add if it does not exist, in case activity is re-used
                if (findPreference(link.id.toString()) == null) {
                    Preference p = new Preference(this);
                    p.setTitle(link.name);
                    p.setKey(link.id.toString());

                    p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, link.name);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, link.url);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, link.id);
                            startActivity(externalWebViewIntent);

                            return true;
                        }
                    });

                    preferenceCategory.addPreference(p);
                }
            }
        }
    }

    /**
     * Load upload path set on preferences
     */
    private void loadInstantUploadPath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadPath = appPrefs.getString(PreferenceKeys.INSTANT_UPLOAD_PATH, getString(R.string.instant_upload_path));
        mPrefInstantUploadPath.setSummary(mUploadPath);
    }

    /**
     * Save storage path
     */
    private void saveStoragePath(String newStoragePath) {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStoragePath = newStoragePath;
        MainApp.setStoragePath(mStoragePath);
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(PreferenceKeys.STORAGE_PATH, mStoragePath);
        editor.apply();
        String storageDescription = DataStorageProvider.getInstance().getStorageDescriptionByPath(mStoragePath);
        mPrefStoragePath.setSummary(storageDescription);
        mPrefStoragePath.setValue(newStoragePath);
    }

    /**
     * Load storage path set on preferences
     */
    private void loadStoragePath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStoragePath = appPrefs.getString(PreferenceKeys.STORAGE_PATH, Environment.getExternalStorageDirectory()
                                                         .getAbsolutePath());
        String storageDescription = DataStorageProvider.getInstance().getStorageDescriptionByPath(mStoragePath);
        mPrefStoragePath.setSummary(storageDescription);
    }

    /**
     * Save the "Instant Upload Path" on preferences
     */
    private void saveInstantUploadPathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(PreferenceKeys.INSTANT_UPLOAD_PATH, mUploadPath);
        editor.apply();
    }

    /**
     * Load upload video path set on preferences
     */
    private void loadInstantUploadVideoPath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadVideoPath = appPrefs.getString("instant_video_upload_path", getString(R.string.instant_upload_path));
        mPrefInstantVideoUploadPath.setSummary(mUploadVideoPath);
    }

    /**
     * Save the "Instant Video Upload Path" on preferences
     */
    private void saveInstantUploadVideoPathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(PreferenceKeys.INSTANT_VIDEO_UPLOAD_PATH, mUploadVideoPath);
        editor.apply();
    }

    @Override
    public void onStorageMigrationFinished(String storagePath, boolean succeed) {
        if (succeed) {
            saveStoragePath(storagePath);
        }
    }

    @Override
    public void onCancelMigration() {
        // Migration was canceled so we don't do anything
    }
}
