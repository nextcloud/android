/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *   @author TSI-mc
 *
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *   Copyright (C) 2023 TSI-mc
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.etm.EtmActivity;
import com.nextcloud.client.logger.ui.LogsActivity;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.client.preferences.DarkMode;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.ExternalLinksProvider;
import com.owncloud.android.datastorage.DataStorageProvider;
import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.ui.ThemeableSwitchPreference;
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask;
import com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DeviceCredentialUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.CapabilityUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

/**
 * An Activity that allows the user to change the application's settings.
 * <p>
 * It proxies the necessary calls via {@link androidx.appcompat.app.AppCompatDelegate} to be used with AppCompat.
 */
public class SettingsActivity extends PreferenceActivity
    implements StorageMigration.StorageMigrationProgressListener,
    LoadingVersionNumberTask.VersionDevInterface,
    Injectable {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREFERENCE_LOCK = "lock";

    public static final String LOCK_NONE = "none";
    public static final String LOCK_PASSCODE = "passcode";
    public static final String LOCK_DEVICE_CREDENTIALS = "device_credentials";


    public final static String PREFERENCE_USE_FINGERPRINT = "use_fingerprint";
    public static final String PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS = "show_media_scan_notifications";

    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;
    private static final int ACTION_CONFIRM_DEVICE_CREDENTIALS = 7;
    private static final int ACTION_REQUEST_CODE_DAVDROID_SETUP = 10;
    private static final int ACTION_SHOW_MNEMONIC = 11;
    private static final int ACTION_E2E = 12;
    private static final int TRUE_VALUE = 1;

    private static final String DAV_PATH = "/remote.php/dav";

    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI";

    private Uri serverBaseUri;

    private ListPreference lock;
    private ThemeableSwitchPreference showHiddenFiles;
    private ThemeableSwitchPreference showEcosystemApps;
    private AppCompatDelegate delegate;

    private ListPreference prefStoragePath;
    private String storagePath;
    private String pendingLock;

    private User user;
    @Inject ArbitraryDataProvider arbitraryDataProvider;
    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject ConnectivityService connectivityService;


    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        setupActionBar();

        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        String appVersion = getAppVersion();
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preference_screen");

        user = accountManager.getUser();

        // retrieve user's base uri
        setupBaseUri();

        // General
        setupGeneralCategory();

        // Synced folders
        setupAutoUploadCategory(preferenceScreen);

        // Details
        setupDetailsCategory(preferenceScreen);

        // More
        setupMoreCategory();

        // About
        setupAboutCategory(appVersion);

        // Dev
        setupDevCategory(preferenceScreen);

        // workaround for mismatched color when app dark mode and system dark mode don't agree
        setListBackground();
    }

    private void setupDevCategory(PreferenceScreen preferenceScreen) {
        // Dev category
        PreferenceCategory preferenceCategoryDev = (PreferenceCategory) findPreference("dev_category");

        if (getResources().getBoolean(R.bool.is_beta)) {
            viewThemeUtils.files.themePreferenceCategory(preferenceCategoryDev);

            /* Link to dev apks */
            Preference pDevLink = findPreference("dev_link");
            if (pDevLink != null) {
                if (getResources().getBoolean(R.bool.dev_version_direct_download_enabled)) {
                    pDevLink.setOnPreferenceClickListener(preference -> {
                        FileActivity.checkForNewDevVersion(this, getApplicationContext());
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
                    DisplayUtils.startLinkIntent(this, R.string.dev_changelog);
                    return true;
                });
            }

            /* Engineering Test Mode */
            Preference pEtm = findPreference("etm");
            if (pEtm != null) {
                pEtm.setOnPreferenceClickListener(preference -> {
                    EtmActivity.launch(this);
                    return true;
                });
            }
        } else {
            preferenceScreen.removePreference(preferenceCategoryDev);
        }
    }

    private void setupAboutCategory(String appVersion) {
        final PreferenceCategory preferenceCategoryAbout = (PreferenceCategory) findPreference("about");
        viewThemeUtils.files.themePreferenceCategory(preferenceCategoryAbout);

        /* About App */
        Preference pAboutApp = findPreference("about_app");
        if (pAboutApp != null) {
            pAboutApp.setTitle(String.format(getString(R.string.about_android), getString(R.string.app_name)));

            String buildNumber = getResources().getString(R.string.buildNumber);

            if (TextUtils.isEmpty(buildNumber)) {
                pAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
            } else {
                pAboutApp.setSummary(String.format(getString(R.string.about_version_with_build),
                                                   appVersion,
                                                   buildNumber));
            }
        }

        // license
        boolean licenseEnabled = getResources().getBoolean(R.bool.license_enabled);
        Preference licensePreference = findPreference("license");
        if (licensePreference != null) {
            if (licenseEnabled) {
                licensePreference.setSummary(R.string.prefs_gpl_v2);
                licensePreference.setOnPreferenceClickListener(preference -> {
                    DisplayUtils.startLinkIntent(this, R.string.license_url);
                    return true;
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
                privacyPreference.setOnPreferenceClickListener(preference -> {
                    try {
                        Uri privacyUrl = Uri.parse(getString(R.string.privacy_url));
                        String mimeType = MimeTypeUtil.getBestMimeTypeByFilename(privacyUrl.getLastPathSegment());

                        Intent intent;
                        if (MimeTypeUtil.isPDF(mimeType)) {
                            intent = new Intent(Intent.ACTION_VIEW, privacyUrl);
                            DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_pdf_app_available);
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
                });
            } else {
                preferenceCategoryAbout.removePreference(privacyPreference);
            }
        }

        // source code
        boolean sourcecodeEnabled = getResources().getBoolean(R.bool.sourcecode_enabled);
        Preference sourcecodePreference = findPreference("sourcecode");
        if (sourcecodePreference != null) {
            if (sourcecodeEnabled) {
                sourcecodePreference.setOnPreferenceClickListener(preference -> {
                    DisplayUtils.startLinkIntent(this, R.string.sourcecode_url);
                    return true;
                });
            } else {
                preferenceCategoryAbout.removePreference(sourcecodePreference);
            }
        }
    }

    private void setupMoreCategory() {
        final PreferenceCategory preferenceCategoryMore = (PreferenceCategory) findPreference("more");
        viewThemeUtils.files.themePreferenceCategory(preferenceCategoryMore);

        setupAutoUploadPreference(preferenceCategoryMore);

        setupCalendarPreference(preferenceCategoryMore);

        setupBackupPreference();

        setupE2EPreference(preferenceCategoryMore);

        setupE2EKeysExist(preferenceCategoryMore);

        setupE2EMnemonicPreference(preferenceCategoryMore);

        removeE2E(preferenceCategoryMore);

        setupHelpPreference(preferenceCategoryMore);

        setupRecommendPreference(preferenceCategoryMore);

        setupLoggingPreference(preferenceCategoryMore);

        setupImprintPreference(preferenceCategoryMore);

        loadExternalSettingLinks(preferenceCategoryMore);
    }

    private void setupImprintPreference(PreferenceCategory preferenceCategoryMore) {
        boolean imprintEnabled = getResources().getBoolean(R.bool.imprint_enabled);
        Preference pImprint = findPreference("imprint");
        if (pImprint != null) {
            if (imprintEnabled) {
                pImprint.setOnPreferenceClickListener(preference -> {
                    String imprintWeb = getString(R.string.url_imprint);

                    if (!imprintWeb.isEmpty()) {
                        DisplayUtils.startLinkIntent(this, imprintWeb);
                    }
                    //ImprintDialog.newInstance(true).show(preference.get, "IMPRINT_DIALOG");
                    return true;
                });
            } else {
                preferenceCategoryMore.removePreference(pImprint);
            }
        }
    }

    private void setupLoggingPreference(PreferenceCategory preferenceCategoryMore) {

        boolean loggerEnabled = getResources().getBoolean(R.bool.logger_enabled) || BuildConfig.DEBUG;
        Preference pLogger = findPreference("logger");
        if (pLogger != null) {
            if (loggerEnabled) {
                pLogger.setOnPreferenceClickListener(preference -> {
                    Intent loggerIntent = new Intent(getApplicationContext(), LogsActivity.class);
                    startActivity(loggerIntent);

                    return true;
                });
            } else {
                preferenceCategoryMore.removePreference(pLogger);
            }
        }
    }


    private void setupRecommendPreference(PreferenceCategory preferenceCategoryMore) {
        boolean recommendEnabled = getResources().getBoolean(R.bool.recommend_enabled);
        Preference pRecommend = findPreference("recommend");
        if (pRecommend != null) {
            if (recommendEnabled) {
                pRecommend.setOnPreferenceClickListener(preference -> {

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

                });
            } else {
                preferenceCategoryMore.removePreference(pRecommend);
            }
        }
    }

    private void setupE2EPreference(PreferenceCategory preferenceCategoryMore) {
        Preference preference = findPreference("setup_e2e");

        if (preference != null) {
            if (FileOperationsHelper.isEndToEndEncryptionSetup(this, user) ||
                CapabilityUtils.getCapability(this).getEndToEndEncryptionKeysExist().isTrue() ||
                CapabilityUtils.getCapability(this).getEndToEndEncryptionKeysExist().isUnknown()
            ) {
                preferenceCategoryMore.removePreference(preference);
            } else {
                preference.setOnPreferenceClickListener(p -> {
                    if (connectivityService.getConnectivity().isConnected()) {
                        Intent i = new Intent(MainApp.getAppContext(), SetupEncryptionActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        i.putExtra("EXTRA_USER", user);
                        startActivityForResult(i, ACTION_E2E);
                    } else {
                        DisplayUtils.showSnackMessage(this, R.string.e2e_offline);
                    }

                    return true;
                });
            }
        }
    }

    private void setupE2EKeysExist(PreferenceCategory preferenceCategoryMore) {
        Preference preference = findPreference("setup_e2e_keys_exist");

        if (preference != null) {
            if (!CapabilityUtils.getCapability(this).getEndToEndEncryptionKeysExist().isTrue() ||
                (CapabilityUtils.getCapability(this).getEndToEndEncryptionKeysExist().isTrue() &&
                    FileOperationsHelper.isEndToEndEncryptionSetup(this, user))) {
                preferenceCategoryMore.removePreference(preference);
            } else {
                preference.setOnPreferenceClickListener(p -> {
                    Intent i = new Intent(MainApp.getAppContext(), SetupEncryptionActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    i.putExtra("EXTRA_USER", user);
                    startActivityForResult(i, ACTION_E2E);

                    return true;
                });
            }
        }
    }

    private void setupE2EMnemonicPreference(PreferenceCategory preferenceCategoryMore) {
        String mnemonic = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.MNEMONIC).trim();

        Preference pMnemonic = findPreference("mnemonic");
        if (pMnemonic != null) {
            if (!mnemonic.isEmpty()) {
                if (DeviceCredentialUtils.areCredentialsAvailable(this)) {
                    pMnemonic.setOnPreferenceClickListener(preference -> {

                        Intent i = new Intent(MainApp.getAppContext(), RequestCredentialsActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivityForResult(i, ACTION_SHOW_MNEMONIC);

                        return true;
                    });
                } else {
                    pMnemonic.setEnabled(false);
                    pMnemonic.setSummary(R.string.prefs_e2e_no_device_credentials);
                }
            } else {
                preferenceCategoryMore.removePreference(pMnemonic);
            }
        }
    }

    private void removeE2E(PreferenceCategory preferenceCategoryMore) {
        Preference preference = findPreference("remove_e2e");

        if (preference != null) {
            if (!FileOperationsHelper.isEndToEndEncryptionSetup(this, user)) {
                preferenceCategoryMore.removePreference(preference);
            } else {
                preference.setOnPreferenceClickListener(p -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FallbackTheming_Dialog);
                    AlertDialog alertDialog = builder.setTitle(R.string.prefs_e2e_mnemonic)
                        .setMessage(getString(R.string.remove_e2e_message))
                        .setCancelable(true)
                        .setNegativeButton(R.string.common_cancel, ((dialog, i) -> dialog.dismiss()))
                        .setPositiveButton(R.string.confirm_removal, (dialog, which) -> {
                            EncryptionUtils.removeE2E(arbitraryDataProvider, user);
                            preferenceCategoryMore.removePreference(preference);

                            Preference pMnemonic = findPreference("mnemonic");
                            if (pMnemonic != null) {
                                preferenceCategoryMore.removePreference(pMnemonic);
                            }

                            dialog.dismiss();
                        })
                        .create();

                    alertDialog.show();

                    return true;
                });
            }
        }
    }

    private void setupHelpPreference(PreferenceCategory preferenceCategoryMore) {
        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp = findPreference("help");
        if (pHelp != null) {
            if (helpEnabled) {
                pHelp.setOnPreferenceClickListener(preference -> {
                    DisplayUtils.startLinkIntent(this, R.string.url_help);
                    return true;
                });
            } else {
                preferenceCategoryMore.removePreference(pHelp);
            }
        }
    }

    private void setupAutoUploadPreference(PreferenceCategory preferenceCategoryMore) {
        Preference autoUpload = findPreference("syncedFolders");
        if (getResources().getBoolean(R.bool.syncedFolder_light)) {
            preferenceCategoryMore.removePreference(autoUpload);
        } else {
            autoUpload.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(this, SyncedFoldersActivity.class);
                startActivity(intent);
                return true;
            });
        }
    }

    private void setupBackupPreference() {
        Preference pContactsBackup = findPreference("backup");
        if (pContactsBackup != null) {
            boolean showCalendarBackup = getResources().getBoolean(R.bool.show_calendar_backup);
            pContactsBackup.setTitle(showCalendarBackup
                                         ? getString(R.string.backup_title)
                                         : getString(R.string.contact_backup_title));
            pContactsBackup.setSummary(showCalendarBackup
                                           ? getString(R.string.prefs_daily_backup_summary)
                                           : getString(R.string.prefs_daily_contact_backup_summary));
            pContactsBackup.setOnPreferenceClickListener(preference -> {
                ContactsPreferenceActivity.startActivityWithoutSidebar(this);
                return true;
            });
        }
    }

    private void setupCalendarPreference(PreferenceCategory preferenceCategoryMore) {
        boolean calendarContactsEnabled = getResources().getBoolean(R.bool.davdroid_integration_enabled);
        Preference pCalendarContacts = findPreference("calendar_contacts");
        if (pCalendarContacts != null) {
            if (calendarContactsEnabled) {
                final Activity activity = this;
                pCalendarContacts.setOnPreferenceClickListener(preference -> {
                    try {
                        launchDavDroidLogin();
                    } catch (Throwable t) {
                        Log_OC.e(TAG, "Error while setting up DavX5", t);
                        DisplayUtils.showSnackMessage(
                            activity,
                            R.string.prefs_davx5_setup_error);
                    }
                    return true;
                });
            } else {
                preferenceCategoryMore.removePreference(pCalendarContacts);
            }
        }
    }

    private void setupDetailsCategory(PreferenceScreen preferenceScreen) {
        PreferenceCategory preferenceCategoryDetails = (PreferenceCategory) findPreference("details");
        viewThemeUtils.files.themePreferenceCategory(preferenceCategoryDetails);

        boolean fPassCodeEnabled = getResources().getBoolean(R.bool.passcode_enabled);
        boolean fDeviceCredentialsEnabled = getResources().getBoolean(R.bool.device_credentials_enabled);
        boolean fShowHiddenFilesEnabled = getResources().getBoolean(R.bool.show_hidden_files_enabled);
        boolean fShowEcosystemAppsEnabled = !getResources().getBoolean(R.bool.is_branded_client);
        boolean fSyncedFolderLightEnabled = getResources().getBoolean(R.bool.syncedFolder_light);
        boolean fShowMediaScanNotifications = preferences.isShowMediaScanNotifications();

        setupLockPreference(preferenceCategoryDetails, fPassCodeEnabled, fDeviceCredentialsEnabled);

        setupHiddenFilesPreference(preferenceCategoryDetails, fShowHiddenFilesEnabled);

        setupShowEcosystemAppsPreference(preferenceCategoryDetails, fShowEcosystemAppsEnabled);

        setupShowMediaScanNotifications(preferenceCategoryDetails, fShowMediaScanNotifications);

        if (!fPassCodeEnabled && !fDeviceCredentialsEnabled && !fShowHiddenFilesEnabled && fSyncedFolderLightEnabled
            && fShowMediaScanNotifications) {
            preferenceScreen.removePreference(preferenceCategoryDetails);
        }
    }

    private void setupShowMediaScanNotifications(PreferenceCategory preferenceCategoryDetails,
                                                 boolean fShowMediaScanNotifications) {
        ThemeableSwitchPreference mShowMediaScanNotifications =
            (ThemeableSwitchPreference) findPreference(PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS);

        if (fShowMediaScanNotifications) {
            preferenceCategoryDetails.removePreference(mShowMediaScanNotifications);
        }
    }

    private void setupHiddenFilesPreference(PreferenceCategory preferenceCategoryDetails,
                                            boolean fShowHiddenFilesEnabled) {
        showHiddenFiles = (ThemeableSwitchPreference) findPreference("show_hidden_files");
        if (fShowHiddenFilesEnabled) {
            showHiddenFiles.setOnPreferenceClickListener(preference -> {
                preferences.setShowHiddenFilesEnabled(showHiddenFiles.isChecked());
                return true;
            });
        } else {
            preferenceCategoryDetails.removePreference(showHiddenFiles);
        }
    }

    private void setupShowEcosystemAppsPreference(PreferenceCategory preferenceCategoryDetails,
                                            boolean fShowEcosystemAppsEnabled) {
        showEcosystemApps = (ThemeableSwitchPreference) findPreference("show_ecosystem_apps");
        if (fShowEcosystemAppsEnabled) {
            showEcosystemApps.setOnPreferenceClickListener(preference -> {
                preferences.setShowEcosystemApps(showEcosystemApps.isChecked());
                return true;
            });
        } else {
            preferenceCategoryDetails.removePreference(showEcosystemApps);
        }
    }


    private void setupLockPreference(PreferenceCategory preferenceCategoryDetails,
                                     boolean passCodeEnabled,
                                     boolean deviceCredentialsEnabled) {
        lock = (ListPreference) findPreference(PREFERENCE_LOCK);
        if (lock != null && (passCodeEnabled || deviceCredentialsEnabled)) {
            ArrayList<String> lockEntries = new ArrayList<>(3);
            lockEntries.add(getString(R.string.prefs_lock_none));
            lockEntries.add(getString(R.string.prefs_lock_using_passcode));
            lockEntries.add(getString(R.string.prefs_lock_using_device_credentials));

            ArrayList<String> lockValues = new ArrayList<>(3);
            lockValues.add(LOCK_NONE);
            lockValues.add(LOCK_PASSCODE);
            lockValues.add(LOCK_DEVICE_CREDENTIALS);

            if (!passCodeEnabled) {
                lockEntries.remove(1);
                lockValues.remove(1);
            } else if (!deviceCredentialsEnabled ||
                !DeviceCredentialUtils.areCredentialsAvailable(getApplicationContext())) {
                lockEntries.remove(2);
                lockValues.remove(2);
            }
            String[] lockEntriesArr = new String[lockEntries.size()];
            lockEntriesArr = lockEntries.toArray(lockEntriesArr);
            String[] lockValuesArr = new String[lockValues.size()];
            lockValuesArr = lockValues.toArray(lockValuesArr);

            lock.setEntries(lockEntriesArr);
            lock.setEntryValues(lockValuesArr);
            lock.setSummary(lock.getEntry());
            lock.setOnPreferenceChangeListener((preference, o) -> {
                pendingLock = LOCK_NONE;
                String oldValue = ((ListPreference) preference).getValue();
                String newValue = (String) o;
                if (!oldValue.equals(newValue)) {
                    if (LOCK_NONE.equals(oldValue)) {
                        enableLock(newValue);
                    } else {
                        pendingLock = newValue;
                        disableLock(oldValue);
                    }
                }
                return false;
            });
        } else {
            preferenceCategoryDetails.removePreference(lock);
        }
    }

    private void setupAutoUploadCategory(PreferenceScreen preferenceScreen) {
        final PreferenceCategory preferenceCategorySyncedFolders =
            (PreferenceCategory) findPreference("synced_folders_category");
        viewThemeUtils.files.themePreferenceCategory(preferenceCategorySyncedFolders);

        if (!getResources().getBoolean(R.bool.syncedFolder_light)) {
            preferenceScreen.removePreference(preferenceCategorySyncedFolders);
        } else {
            // Upload on WiFi
            final ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(this);

            final SwitchPreference pUploadOnWifiCheckbox = (SwitchPreference) findPreference("synced_folder_on_wifi");
            pUploadOnWifiCheckbox.setChecked(
                arbitraryDataProvider.getBooleanValue(user, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI));

            pUploadOnWifiCheckbox.setOnPreferenceClickListener(preference -> {
                arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI,
                                                            String.valueOf(pUploadOnWifiCheckbox.isChecked()));

                return true;
            });

            Preference pSyncedFolder = findPreference("synced_folders_configure_folders");
            if (pSyncedFolder != null) {
                if (getResources().getBoolean(R.bool.syncedFolder_light)) {
                    pSyncedFolder.setOnPreferenceClickListener(preference -> {
                        Intent intent = new Intent(this, SyncedFoldersActivity.class);
                        startActivity(intent);
                        return true;
                    });
                } else {
                    preferenceCategorySyncedFolders.removePreference(pSyncedFolder);
                }
            }
        }
    }

    private void enableLock(String lock) {
        pendingLock = LOCK_NONE;
        if (LOCK_PASSCODE.equals(lock)) {
            Intent i = new Intent(getApplicationContext(), PassCodeActivity.class);
            i.setAction(PassCodeActivity.ACTION_REQUEST_WITH_RESULT);
            startActivityForResult(i, ACTION_REQUEST_PASSCODE);
        } else if (LOCK_DEVICE_CREDENTIALS.equals(lock)) {
            if (!DeviceCredentialUtils.areCredentialsAvailable(getApplicationContext())) {
                DisplayUtils.showSnackMessage(this, R.string.prefs_lock_device_credentials_not_setup);
            } else {
                DisplayUtils.showSnackMessage(this, R.string.prefs_lock_device_credentials_enabled);
                changeLockSetting(LOCK_DEVICE_CREDENTIALS);
            }
        }
    }

    private void changeLockSetting(String value) {
        lock.setValue(value);
        lock.setSummary(lock.getEntry());
        DocumentsStorageProvider.notifyRootsChanged(this);
    }

    private void disableLock(String lock) {
        if (LOCK_PASSCODE.equals(lock)) {
            Intent i = new Intent(getApplicationContext(), PassCodeActivity.class);
            i.setAction(PassCodeActivity.ACTION_CHECK_WITH_RESULT);
            startActivityForResult(i, ACTION_CONFIRM_PASSCODE);
        } else if (LOCK_DEVICE_CREDENTIALS.equals(lock)) {
            Intent i = new Intent(getApplicationContext(), RequestCredentialsActivity.class);
            startActivityForResult(i, ACTION_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    private void setupGeneralCategory() {
        final PreferenceCategory preferenceCategoryGeneral = (PreferenceCategory) findPreference("general");
        viewThemeUtils.files.themePreferenceCategory(preferenceCategoryGeneral);

        prefStoragePath = (ListPreference) findPreference(AppPreferencesImpl.STORAGE_PATH);
        if (prefStoragePath != null) {
            StoragePoint[] storageOptions = DataStorageProvider.getInstance().getAvailableStoragePoints();
            String[] entries = new String[storageOptions.length];
            String[] values = new String[storageOptions.length];
            for (int i = 0; i < storageOptions.length; ++i) {
                entries[i] = storageOptions[i].getDescription();
                values[i] = storageOptions[i].getPath();
            }
            prefStoragePath.setEntries(entries);
            prefStoragePath.setEntryValues(values);

            prefStoragePath.setOnPreferenceChangeListener((preference, newValue) -> {
                String newPath = (String) newValue;

                if (storagePath.equals(newPath)) {
                    return true;
                }
                StorageMigration storageMigration = new StorageMigration(this, user, storagePath, newPath);
                storageMigration.setStorageMigrationProgressListener(this);
                storageMigration.migrate();

                return false;
            });
        }

        loadStoragePath();

        ListPreference themePref = (ListPreference) findPreference("darkMode");

        List<String> themeEntries = new ArrayList<>(3);
        themeEntries.add(getString(R.string.prefs_value_theme_light));
        themeEntries.add(getString(R.string.prefs_value_theme_dark));
        themeEntries.add(getString(R.string.prefs_value_theme_system));

        List<String> themeValues = new ArrayList<>(3);
        themeValues.add(DarkMode.LIGHT.name());
        themeValues.add(DarkMode.DARK.name());
        themeValues.add(DarkMode.SYSTEM.name());

        themePref.setEntries(themeEntries.toArray(new String[0]));
        themePref.setEntryValues(themeValues.toArray(new String[0]));

        if (TextUtils.isEmpty(themePref.getEntry())) {
            themePref.setValue(DarkMode.SYSTEM.name());
            themePref.setSummary(TextUtils.isEmpty(themePref.getEntry()) ? DarkMode.SYSTEM.name() : themePref.getEntry());
        }

        themePref.setOnPreferenceChangeListener((preference, newValue) -> {
            DarkMode mode = DarkMode.valueOf((String) newValue);
            preferences.setDarkThemeMode(mode);
            MainApp.setAppTheme(mode);
            setListBackground();

            return true;
        });
    }

    private void setListBackground() {
        getListView().setBackgroundColor(ContextCompat.getColor(this, R.color.bg_default));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        ActionBar actionBar = getDelegate().getSupportActionBar();

        if (actionBar != null) {
            viewThemeUtils.platform.themeStatusBar(this);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            if (this.getResources() != null) {
                viewThemeUtils.androidx.themeActionBar(this,
                                                       actionBar,
                                                       getString(R.string.actionbar_settings),
                                                       ResourcesCompat.getDrawable(this.getResources(),
                                                                                   R.drawable.ic_arrow_back,
                                                                                   null));
            }
        }
    }

    private void launchDavDroidLogin() {
        Intent davDroidLoginIntent = new Intent();
        davDroidLoginIntent.setClassName("at.bitfire.davdroid", "at.bitfire.davdroid.ui.setup.LoginActivity");
        if (getPackageManager().resolveActivity(davDroidLoginIntent, 0) != null) {
            // arguments
            if (serverBaseUri != null) {
                davDroidLoginIntent.putExtra("url", serverBaseUri + DAV_PATH);

                davDroidLoginIntent.putExtra("loginFlow", TRUE_VALUE);
                davDroidLoginIntent.setData(Uri.parse(serverBaseUri.toString() + AuthenticatorActivity.WEB_LOGIN));
                davDroidLoginIntent.putExtra("davPath", DAV_PATH);
            }
            davDroidLoginIntent.putExtra("username", UserAccountManager.getUsername(user));

            startActivityForResult(davDroidLoginIntent, ACTION_REQUEST_CODE_DAVDROID_SETUP);
        } else {
            // DAVdroid not installed
            Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=at.bitfire.davdroid"));

            // launch market(s)
            if (installIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(installIntent);
            } else {
                // no f-droid market app or Play store installed --> launch browser for f-droid url
                DisplayUtils.startLinkIntent(this, "https://f-droid.org/packages/at.bitfire.davdroid/");

                DisplayUtils.showSnackMessage(this, R.string.prefs_calendar_contacts_no_store_error);
            }
        }
    }

    private void setupBaseUri() {
        // retrieve and set user's base URI
        Thread t = new Thread(() -> {
            try {
                serverBaseUri = clientFactory.create(user).getBaseUri();
            } catch (Exception e) {
                Log_OC.e(TAG, "Error retrieving user's base URI", e);
            }
        });
        t.start();
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
                appPrefs.apply();
                changeLockSetting(LOCK_PASSCODE);
                DisplayUtils.showSnackMessage(this, R.string.pass_code_stored);
            }
        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {
                changeLockSetting(LOCK_NONE);

                DisplayUtils.showSnackMessage(this, R.string.pass_code_removed);
                if (!LOCK_NONE.equals(pendingLock)) {
                    enableLock(pendingLock);
                }
            }
        } else if (requestCode == ACTION_REQUEST_CODE_DAVDROID_SETUP && resultCode == RESULT_OK) {
            DisplayUtils.showSnackMessage(this, R.string.prefs_calendar_contacts_sync_setup_successful);
        } else if (requestCode == ACTION_CONFIRM_DEVICE_CREDENTIALS && resultCode == RESULT_OK &&
            data.getIntExtra(RequestCredentialsActivity.KEY_CHECK_RESULT,
                             RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE) ==
                RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE) {
            changeLockSetting(LOCK_NONE);
            DisplayUtils.showSnackMessage(this, R.string.credentials_disabled);
            if (!LOCK_NONE.equals(pendingLock)) {
                enableLock(pendingLock);
            }
        } else if (requestCode == ACTION_SHOW_MNEMONIC && resultCode == RESULT_OK) {
            handleMnemonicRequest(data);
        } else if (requestCode == ACTION_E2E && data != null && data.getBooleanExtra(SetupEncryptionDialogFragment.SUCCESS, false)) {
            Intent i = new Intent(this, SettingsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(i);
        }
    }

    @VisibleForTesting
    public void handleMnemonicRequest(Intent data) {
        if (data == null) {
            DisplayUtils.showSnackMessage(this, "Error retrieving mnemonic!");
        } else {
            if (data.getIntExtra(RequestCredentialsActivity.KEY_CHECK_RESULT,
                                 RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE) ==
                RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE) {

                ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(this);
                String mnemonic = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.MNEMONIC).trim();

                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FallbackTheming_Dialog);
                AlertDialog alertDialog = builder.setTitle(R.string.prefs_e2e_mnemonic)
                    .setMessage(mnemonic)
                    .setNegativeButton(R.string.common_cancel, (dialog, i) -> dialog.dismiss())
                    .setNeutralButton(R.string.common_copy, (dialog, i) ->
                        ClipboardUtil.copyToClipboard(this, mnemonic, false))
                    .setPositiveButton(R.string.common_ok, (dialog, which) -> dialog.dismiss())
                    .create();

                alertDialog.show();
                viewThemeUtils.platform.colorTextButtons(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE));
            }
        }
    }

    @Override
    @NonNull
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
        if (delegate == null) {
            delegate = AppCompatDelegate.create(this, null);
        }
        return delegate;
    }

    private void loadExternalSettingLinks(PreferenceCategory preferenceCategory) {
        if (getBaseContext().getResources().getBoolean(R.bool.show_external_links)) {
            ExternalLinksProvider externalLinksProvider = new ExternalLinksProvider(getContentResolver());

            for (final ExternalLink link : externalLinksProvider.getExternalLink(ExternalLinkType.SETTINGS)) {

                // only add if it does not exist, in case activity is re-used
                if (findPreference(String.valueOf(link.getId())) == null) {
                    Preference p = new Preference(this);
                    p.setTitle(link.getName());
                    p.setKey(String.valueOf(link.getId()));

                    p.setOnPreferenceClickListener(preference -> {
                        Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, link.getName());
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, link.getUrl());
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, link.getId());
                        startActivity(externalWebViewIntent);

                        return true;
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
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        storagePath = newStoragePath;
        MainApp.setStoragePath(storagePath);
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(AppPreferencesImpl.STORAGE_PATH, storagePath);
        editor.apply();
        String storageDescription = DataStorageProvider.getInstance().getStorageDescriptionByPath(storagePath);
        prefStoragePath.setSummary(storageDescription);
        prefStoragePath.setValue(newStoragePath);
    }

    /**
     * Load storage path set on preferences
     */
    private void loadStoragePath() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // Load storage path from shared preferences. Use private internal storage by default.
        storagePath = appPrefs.getString(AppPreferencesImpl.STORAGE_PATH,
                                         getApplicationContext().getFilesDir().getAbsolutePath());
        String storageDescription = DataStorageProvider.getInstance().getStorageDescriptionByPath(storagePath);
        prefStoragePath.setSummary(storageDescription);
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

    @Override
    public void returnVersion(Integer latestVersion) {
        FileActivity.showDevSnackbar(this, latestVersion, true, false);
    }
}
