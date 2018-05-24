/*
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
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ExternalLinksProvider;
import com.owncloud.android.datastorage.DataStorageProvider;
import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.IOException;

/**
 * An Activity that allows the user to change the application's settings.
 *
 * It proxies the necessary calls via {@link android.support.v7.app.AppCompatDelegate} to be used with AppCompat.
 */
public class Preferences extends PreferenceActivity
        implements StorageMigration.StorageMigrationProgressListener {

    private static final String TAG = Preferences.class.getSimpleName();

    public final static String PREFERENCE_USE_FINGERPRINT = "use_fingerprint";
    public static final String PREFERENCE_EXPERT_MODE = "expert_mode";

    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;

    private static final int ACTION_REQUEST_CODE_DAVDROID_SETUP = 10;

    private static final String DAV_PATH = "/remote.php/dav";

    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI";

    /**
     * The user's server base uri.
     */
    private Uri mUri;

    private SwitchPreference pCode;
    private SwitchPreference fPrint;
    private SwitchPreference mShowHiddenFiles;
    private SwitchPreference mExpertMode;
    private AppCompatDelegate mDelegate;

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

        if (ThemeUtils.themingEnabled(this)) {
            setTheme(R.style.FallbackThemingTheme);
        }

        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // retrieve user's base uri
        setupBaseUri();

        setupActionBar();

        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        int accentColor = ThemeUtils.primaryAccentColor(this);
        String appVersion = getAppVersion();
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preference_screen");

        // General
        setupGeneralCategory(accentColor);

        // Synced folders
        setupAutoUploadCategory(accentColor, preferenceScreen);

        // Details
        setupDetailsCategory(accentColor, preferenceScreen);

        // More
        setupMoreCategory(accentColor, appVersion);

        // About
        setupAboutCategory(accentColor, appVersion);
        
        // Dev
        setupDevCategory(accentColor, preferenceScreen);
    }

    private void setupDevCategory(int accentColor, PreferenceScreen preferenceScreen) {
        // Dev category
        PreferenceCategory preferenceCategoryDev = (PreferenceCategory) findPreference("dev_category");

        if (getResources().getBoolean(R.bool.is_beta)) {
            preferenceCategoryDev.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_dev),
                    accentColor));

            /* Link to dev apks */
            Preference pDevLink = findPreference("dev_link");
            if (pDevLink != null) {
                if (getResources().getBoolean(R.bool.dev_version_direct_download_enabled)) {
                    pDevLink.setOnPreferenceClickListener(preference -> {
                        FileActivity.checkForNewDevVersion(getListView(), getApplicationContext(), true);
                        return true;
                    });
                } else {
                    preferenceCategoryDev.removePreference(pDevLink);
                }
            }

            /* Link to dev changelog */
            Preference pChangelogLink = findPreference("changelog_link");
            if (pChangelogLink != null) {
                pChangelogLink.setOnPreferenceClickListener(preference -> {
                    String devChangelogLink = getString(R.string.dev_changelog);
                    Uri uriUrl = Uri.parse(devChangelogLink);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                    startActivity(intent);
                    return true;
                });
            }
        } else {
            preferenceScreen.removePreference(preferenceCategoryDev);
        }
    }

    private void setupAboutCategory(int accentColor, String appVersion) {
        PreferenceCategory preferenceCategoryAbout = (PreferenceCategory) findPreference("about");
        preferenceCategoryAbout.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_about),
                accentColor));

        /* About App */
        Preference pAboutApp = findPreference("about_app");
        if (pAboutApp != null) {
            pAboutApp.setTitle(String.format(getString(R.string.about_android), getString(R.string.app_name)));
            pAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
        }

        // license
        boolean licenseEnabled = getResources().getBoolean(R.bool.license_enabled);
        Preference licensePreference = findPreference("license");
        if (licensePreference != null) {
            String licenseUrl = getString(R.string.license_url);

            if (licenseEnabled && !licenseUrl.isEmpty()) {
                licensePreference.setSummary(R.string.prefs_gpl_v2);
                licensePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri uriUrl = Uri.parse(licenseUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        startActivity(intent);
                        return true;
                    }
                });
            } else {
                preferenceCategoryAbout.removePreference(licensePreference);
            }
        }

        // privacy
        boolean privacyEnabled = getResources().getBoolean(R.bool.privacy_enabled);
        Preference privacyPreference = findPreference("privacy");
        if (privacyPreference != null) {
            if (privacyEnabled && URLUtil.isValidUrl(getString(R.string.privacy_url))) {
                privacyPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            Uri privacyUrl = Uri.parse(getString(R.string.privacy_url));
                            String mimeType = MimeTypeUtil.getBestMimeTypeByFilename(privacyUrl.getLastPathSegment());
                            
                            Intent intent;
                            if ("application/pdf".equals(mimeType)) {
                                intent = new Intent(Intent.ACTION_VIEW, privacyUrl);
                            } else {
                                intent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                                intent.putExtra(ExternalSiteWebView.EXTRA_TITLE, 
                                        getResources().getString(R.string.privacy));
                                intent.putExtra(ExternalSiteWebView.EXTRA_URL, privacyUrl.toString());
                                intent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                                intent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1);
                            }
                            
                            startActivity(intent);
                        } catch (Exception e) {
                            Log_OC.e(TAG, "Could not parse privacy url");
                            preferenceCategoryAbout.removePreference(privacyPreference);
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategoryAbout.removePreference(privacyPreference);
            }
        }

        // source code
        boolean sourcecodeEnabled = getResources().getBoolean(R.bool.sourcecode_enabled);
        Preference sourcecodePreference = findPreference("sourcecode");
        if (sourcecodePreference != null) {
            String sourcecodeUrl = getString(R.string.sourcecode_url);
            if (sourcecodeEnabled && !sourcecodeUrl.isEmpty()) {
                sourcecodePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri uriUrl = Uri.parse(sourcecodeUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        startActivity(intent);
                        return true;
                    }
                });
            } else {
                preferenceCategoryAbout.removePreference(sourcecodePreference);
            }
        }
    }

    private void setupMoreCategory(int accentColor, String appVersion) {
        PreferenceCategory preferenceCategoryMore = (PreferenceCategory) findPreference("more");
        preferenceCategoryMore.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_more),
                accentColor));

        setupCalendarPreference(preferenceCategoryMore);

        setupContactsBackupPreference(preferenceCategoryMore);

        setupHelpPreference(preferenceCategoryMore);

        setupRecommendPreference(preferenceCategoryMore);

        setupFeedbackPreference(appVersion, preferenceCategoryMore);

        setupLoggingPreference(preferenceCategoryMore);

        setupImprintPreference(preferenceCategoryMore);

        loadExternalSettingLinks(preferenceCategoryMore);
    }

    private void setupImprintPreference(PreferenceCategory preferenceCategoryMore) {
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
    }

    private void setupLoggingPreference(PreferenceCategory preferenceCategoryMore) {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean loggerEnabled = getResources().getBoolean(R.bool.logger_enabled) || BuildConfig.DEBUG ||
                appPrefs.getBoolean(PREFERENCE_EXPERT_MODE, false);
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
    }

    private void setupFeedbackPreference(String appVersion, PreferenceCategory preferenceCategoryMore) {
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
    }

    private void setupRecommendPreference(PreferenceCategory preferenceCategoryMore) {
        boolean recommendEnabled = getResources().getBoolean(R.bool.recommend_enabled);
        Preference pRecommend = findPreference("recommend");
        if (pRecommend != null) {
            if (recommendEnabled) {
                pRecommend.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        String appName = getString(R.string.app_name);
                        String downloadUrlGooglePlayStore = getString(R.string.url_app_download);
                        String downloadUrlFDroid = getString(R.string.fdroid_link);
                        String downloadUrls = String.format(getString(R.string.recommend_urls),
                                downloadUrlGooglePlayStore, downloadUrlFDroid);

                        String recommendSubject = String.format(getString(R.string.recommend_subject), appName);
                        String recommendText = String.format(getString(R.string.recommend_text),
                                appName, downloadUrls);

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
    }

    private void setupHelpPreference(PreferenceCategory preferenceCategoryMore) {
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
    }

    private void setupContactsBackupPreference(PreferenceCategory preferenceCategoryMore) {
        boolean contactsBackupEnabled = !getResources().getBoolean(R.bool.show_drawer_contacts_backup)
                && getResources().getBoolean(R.bool.contacts_backup);
        Preference pContactsBackup = findPreference("contacts");
        if (pContactsBackup != null) {
            if (contactsBackupEnabled) {
                pContactsBackup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent contactsIntent = new Intent(getApplicationContext(), ContactsPreferenceActivity.class);
                        contactsIntent.putExtra(ContactsPreferenceActivity.EXTRA_SHOW_SIDEBAR, false);
                        startActivity(contactsIntent);
                        return true;
                    }
                });
            } else {
                preferenceCategoryMore.removePreference(pContactsBackup);
            }
        }
    }

    private void setupCalendarPreference(PreferenceCategory preferenceCategoryMore) {
        boolean calendarContactsEnabled = getResources().getBoolean(R.bool.davdroid_integration_enabled);
        Preference pCalendarContacts = findPreference("calendar_contacts");
        if (pCalendarContacts != null) {
            if (calendarContactsEnabled) {
                final Activity activity = this;
                pCalendarContacts.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            launchDavDroidLogin();
                        } catch (Throwable t) {
                            Log_OC.e(TAG, "Base Uri for account could not be resolved to call DAVdroid!", t);
                            DisplayUtils.showSnackMessage(
                                    activity,
                                    R.string.prefs_calendar_contacts_address_resolve_error
                            );
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategoryMore.removePreference(pCalendarContacts);
            }
        }
    }

    private void setupDetailsCategory(int accentColor, PreferenceScreen preferenceScreen) {
        PreferenceCategory preferenceCategoryDetails = (PreferenceCategory) findPreference("details");
        preferenceCategoryDetails.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_details),
                accentColor));

        boolean fPassCodeEnabled = getResources().getBoolean(R.bool.passcode_enabled);
        boolean fPrintEnabled = getResources().getBoolean(R.bool.fingerprint_enabled);
        boolean fShowHiddenFilesEnabled = getResources().getBoolean(R.bool.show_hidden_files_enabled);

        setupPasscodePreference(preferenceCategoryDetails, fPassCodeEnabled);

        setupFingerprintPreference(preferenceCategoryDetails, fPrintEnabled);

        setupHiddenFilesPreference(preferenceCategoryDetails, fShowHiddenFilesEnabled);

        setupExpertModePreference(preferenceCategoryDetails);

        if (!fShowHiddenFilesEnabled && !fPrintEnabled && !fPassCodeEnabled) {
            preferenceScreen.removePreference(preferenceCategoryDetails);
        }
    }

    private void setupExpertModePreference(PreferenceCategory preferenceCategoryDetails) {
        mExpertMode = (SwitchPreference) findPreference(PREFERENCE_EXPERT_MODE);

        if (getResources().getBoolean(R.bool.syncedFolder_light)) {
            preferenceCategoryDetails.removePreference(mExpertMode);
        } else {
            mExpertMode = (SwitchPreference) findPreference(PREFERENCE_EXPERT_MODE);
            mExpertMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences appPrefs =
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = appPrefs.edit();
                    editor.putBoolean(PREFERENCE_EXPERT_MODE, mExpertMode.isChecked());
                    editor.apply();

                    if (mExpertMode.isChecked()) {
                        Log_OC.startLogging(getApplicationContext());
                    } else {
                        if (!BuildConfig.DEBUG &&
                                !getApplicationContext().getResources().getBoolean(R.bool.logger_enabled)) {
                            Log_OC.stopLogging();
                        }
                    }

                    return true;
                }
            });
        }
    }

    private void setupHiddenFilesPreference(PreferenceCategory preferenceCategoryDetails,
                                            boolean fShowHiddenFilesEnabled) {
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
    }

    private void setupFingerprintPreference(PreferenceCategory preferenceCategoryDetails, boolean fPrintEnabled) {
        fPrint = (SwitchPreference) findPreference(PREFERENCE_USE_FINGERPRINT);
        if (fPrint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (FingerprintActivity.isFingerprintCapable(MainApp.getAppContext()) && fPrintEnabled) {
                    final Activity activity = this;
                    fPrint.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            Boolean incoming = (Boolean) newValue;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (FingerprintActivity.isFingerprintReady(MainApp.getAppContext())) {
                                    SharedPreferences appPrefs =
                                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    SharedPreferences.Editor editor = appPrefs.edit();
                                    editor.putBoolean(PREFERENCE_USE_FINGERPRINT, incoming);
                                    editor.apply();
                                    return true;
                                } else {
                                    if (incoming) {
                                        DisplayUtils.showSnackMessage(activity, R.string.prefs_fingerprint_notsetup);
                                        fPrint.setChecked(false);
                                    }
                                    SharedPreferences appPrefs =
                                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    SharedPreferences.Editor editor = appPrefs.edit();
                                    editor.putBoolean(PREFERENCE_USE_FINGERPRINT, false);
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
    }

    private void setupPasscodePreference(PreferenceCategory preferenceCategoryDetails, boolean fPassCodeEnabled) {
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
    }

    private void setupAutoUploadCategory(int accentColor, PreferenceScreen preferenceScreen) {
        PreferenceCategory preferenceCategorySyncedFolders =
                (PreferenceCategory) findPreference("synced_folders_category");
        preferenceCategorySyncedFolders.setTitle(ThemeUtils.getColoredTitle(getString(R.string.drawer_synced_folders),
                accentColor));

        if (!getResources().getBoolean(R.bool.syncedFolder_light)) {
            preferenceScreen.removePreference(preferenceCategorySyncedFolders);
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
                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI,
                            String.valueOf(pUploadOnWifiCheckbox.isChecked()));

                    return true;
                }
            });

            Preference pSyncedFolder = findPreference("synced_folders_configure_folders");
            if (pSyncedFolder != null) {
                if (getResources().getBoolean(R.bool.syncedFolder_light)
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pSyncedFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent syncedFoldersIntent =
                                    new Intent(getApplicationContext(), SyncedFoldersActivity.class);
                            syncedFoldersIntent.putExtra(SyncedFoldersActivity.EXTRA_SHOW_SIDEBAR, false);
                            startActivity(syncedFoldersIntent);
                            return true;
                        }
                    });
                } else {
                    preferenceCategorySyncedFolders.removePreference(pSyncedFolder);
                }
            }
        }
    }

    private void setupGeneralCategory(int accentColor) {
        PreferenceCategory preferenceCategoryGeneral = (PreferenceCategory) findPreference("general");
        preferenceCategoryGeneral.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_general),
                accentColor));

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

        loadStoragePath();
    }

    private String getAppVersion() {
        String temp;
        try {
            PackageInfo pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
            temp = pkg.versionName;
        } catch (NameNotFoundException e) {
            temp = "";
            Log_OC.e(TAG, "Error while showing about dialog", e);
        }
        return temp;
    }

    private void setupActionBar() {
        ActionBar actionBar = getDelegate().getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        ThemeUtils.setColoredTitle(actionBar, getString(R.string.actionbar_settings), this);
        actionBar.setBackgroundDrawable(new ColorDrawable(ThemeUtils.primaryColor(this)));
        getWindow().getDecorView().setBackgroundDrawable(new ColorDrawable(ResourcesCompat
                .getColor(getResources(), R.color.background_color, null)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ThemeUtils.primaryDarkColor(this));
        }

        Drawable backArrow = getResources().getDrawable(R.drawable.ic_arrow_back);
        actionBar.setHomeAsUpIndicator(ThemeUtils.tintDrawable(backArrow, ThemeUtils.fontColor(this)));

        // For adding content description tag to a title field in the action bar
        int actionBarTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
        View actionBarTitleView = getWindow().getDecorView().findViewById(actionBarTitleId);
        if (actionBarTitleView != null) {    // it's null in Android 2.x
            getWindow().getDecorView().findViewById(actionBarTitleId).
                    setContentDescription(getString(R.string.actionbar_settings));
        }
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
                davDroidLoginIntent.putExtra("url", mUri.toString() + DAV_PATH);
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

                DisplayUtils.showSnackMessage(this, R.string.prefs_calendar_contacts_no_store_error);
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
                    Log_OC.e(TAG, "Error retrieving user's base URI", t);
                }
            }
        });
        t.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean state = appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
        pCode.setChecked(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_REQUEST_PASSCODE && resultCode == RESULT_OK) {
            String passcode = data.getStringExtra(PassCodeActivity.KEY_PASSCODE);
            if (passcode != null && passcode.length() == 4) {
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();

                for (int i = 1; i <= 4; ++i) {
                    appPrefs.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i, passcode.substring(i - 1, i));
                }
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true);
                appPrefs.apply();
                DisplayUtils.showSnackMessage(this, R.string.pass_code_stored);
            }
        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {

                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
                appPrefs.apply();

                DisplayUtils.showSnackMessage(this, R.string.pass_code_removed);
            }
        } else if (requestCode == ACTION_REQUEST_CODE_DAVDROID_SETUP && resultCode == RESULT_OK) {
            DisplayUtils.showSnackMessage(this, R.string.prefs_calendar_contacts_sync_setup_successful);
        }
    }

    @NonNull
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
                            Intent externalWebViewIntent =
                                    new Intent(getApplicationContext(), ExternalSiteWebView.class);
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
        // Load storage path from shared preferences. Use private internal storage by default.
        mStoragePath = appPrefs.getString(PreferenceKeys.STORAGE_PATH,
                getApplicationContext().getFilesDir().getAbsolutePath());
        String storageDescription = DataStorageProvider.getInstance().getStorageDescriptionByPath(mStoragePath);
        mPrefStoragePath.setSummary(storageDescription);
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
