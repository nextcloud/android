/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.view.Window;
import android.webkit.URLUtil;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ExternalLinksProvider;
import com.owncloud.android.datastorage.DataStorageProvider;
import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask;
import com.owncloud.android.utils.DeviceCredentialUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;

/**
 * An Activity that allows the user to change the application's settings.
 *
 * It proxies the necessary calls via {@link androidx.appcompat.app.AppCompatDelegate} to be used with AppCompat.
 */
public class SettingsActivity extends PreferenceActivity
    implements StorageMigration.StorageMigrationProgressListener, LoadingVersionNumberTask.VersionDevInterface,
    Injectable {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREFERENCE_LOCK= "lock";

    public static final String LOCK_NONE = "none";
    public static final String LOCK_PASSCODE = "passcode";
    public static final String LOCK_DEVICE_CREDENTIALS = "device_credentials";

    public final static String PREFERENCE_USE_FINGERPRINT = "use_fingerprint";
    public static final String PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS = "show_media_scan_notifications";

    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;
    private static final int ACTION_CONFIRM_DEVICE_CREDENTIALS = 7;
    private static final int ACTION_REQUEST_CODE_DAVDROID_SETUP = 10;

    private static final String DAV_PATH = "/remote.php/dav";

    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI";

    private Uri serverBaseUri;

    private ListPreference lock;
    private SwitchPreference showHiddenFiles;
    private AppCompatDelegate delegate;

    private ListPreference prefStoragePath;
    private String storagePath;
    private String pendingLock;

    private Account account;
    @Inject ArbitraryDataProvider arbitraryDataProvider;
    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;

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

        setupActionBar();

        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        int accentColor = ThemeUtils.primaryAccentColor(this);
        String appVersion = getAppVersion();
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preference_screen");

        account = accountManager.getCurrentAccount();

        // retrieve user's base uri
        setupBaseUri();

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
                        if ("application/pdf".equals(mimeType)) {
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

    private void setupMoreCategory(int accentColor, String appVersion) {
        PreferenceCategory preferenceCategoryMore = (PreferenceCategory) findPreference("more");
        preferenceCategoryMore.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_more),
                accentColor));

        setupCalendarPreference(preferenceCategoryMore);

        setupContactsBackupPreference(preferenceCategoryMore);

        setupE2EMnemonicPreference(preferenceCategoryMore);

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
                pImprint.setOnPreferenceClickListener(preference -> {
                    String imprintWeb = getString(R.string.url_imprint);

                    if (!imprintWeb.isEmpty()) {
                        Uri uriUrl = Uri.parse(imprintWeb);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_browser_available);
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
                    Intent loggerIntent = new Intent(getApplicationContext(), LogHistoryActivity.class);
                    startActivity(loggerIntent);

                    return true;
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
                pFeedback.setOnPreferenceClickListener(preference -> {
                    String feedback = getText(R.string.prefs_feedback) + " - android v" + appVersion;
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "", null));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.mail_feedback)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, feedback);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        DisplayUtils.showSnackMessage(this, R.string.feedback_no_mail_app);
                    }

                    return true;
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

    private void setupE2EMnemonicPreference(PreferenceCategory preferenceCategoryMore) {
        String mnemonic = arbitraryDataProvider.getValue(account.name, EncryptionUtils.MNEMONIC);

        Preference pMnemonic = findPreference("mnemonic");
        if (pMnemonic != null) {
            if (!mnemonic.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (DeviceCredentialUtils.areCredentialsAvailable(this)) {
                    pMnemonic.setOnPreferenceClickListener(preference -> {

                        Intent i = new Intent(MainApp.getAppContext(), RequestCredentialsActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivityForResult(i, PassCodeManager.PASSCODE_ACTIVITY);

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

    private void setupHelpPreference(PreferenceCategory preferenceCategoryMore) {
        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp = findPreference("help");
        if (pHelp != null) {
            if (helpEnabled) {
                pHelp.setOnPreferenceClickListener(preference -> {
                    String helpWeb = getString(R.string.url_help);
                    if (!helpWeb.isEmpty()) {
                        Uri uriUrl = Uri.parse(helpWeb);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_browser_available);
                    }
                    return true;
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
                pContactsBackup.setOnPreferenceClickListener(preference -> {
                    Intent contactsIntent = new Intent(getApplicationContext(), ContactsPreferenceActivity.class);
                    contactsIntent.putExtra(ContactsPreferenceActivity.EXTRA_SHOW_SIDEBAR, false);
                    startActivity(contactsIntent);
                    return true;
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
                pCalendarContacts.setOnPreferenceClickListener(preference -> {
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
        boolean fDeviceCredentialsEnabled = getResources().getBoolean(R.bool.device_credentials_enabled);
        boolean fShowHiddenFilesEnabled = getResources().getBoolean(R.bool.show_hidden_files_enabled);
        boolean fSyncedFolderLightEnabled = getResources().getBoolean(R.bool.syncedFolder_light);
        boolean fShowMediaScanNotifications = preferences.isShowMediaScanNotifications();

        setupLockPreference(preferenceCategoryDetails, fPassCodeEnabled, fDeviceCredentialsEnabled);

        setupHiddenFilesPreference(preferenceCategoryDetails, fShowHiddenFilesEnabled);

        setupShowMediaScanNotifications(preferenceCategoryDetails, fShowMediaScanNotifications);

        if (!fPassCodeEnabled && !fDeviceCredentialsEnabled && !fShowHiddenFilesEnabled && fSyncedFolderLightEnabled
            && fShowMediaScanNotifications) {
            preferenceScreen.removePreference(preferenceCategoryDetails);
        }
    }

    private void setupShowMediaScanNotifications(PreferenceCategory preferenceCategoryDetails,
                                                 boolean fShowMediaScanNotifications) {
        SwitchPreference mShowMediaScanNotifications = (SwitchPreference) findPreference(PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS);

        if (fShowMediaScanNotifications) {
            preferenceCategoryDetails.removePreference(mShowMediaScanNotifications);
        }
    }

    private void setupHiddenFilesPreference(PreferenceCategory preferenceCategoryDetails,
                                            boolean fShowHiddenFilesEnabled) {
        showHiddenFiles = (SwitchPreference) findPreference("show_hidden_files");
        if (fShowHiddenFilesEnabled) {
            showHiddenFiles.setOnPreferenceClickListener(preference -> {
                preferences.setShowHiddenFilesEnabled(showHiddenFiles.isChecked());
                return true;
            });
        } else {
            preferenceCategoryDetails.removePreference(showHiddenFiles);
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
            } else if (!deviceCredentialsEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
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

            final SwitchPreference pUploadOnWifiCheckbox = (SwitchPreference) findPreference("synced_folder_on_wifi");
            pUploadOnWifiCheckbox.setChecked(
                    arbitraryDataProvider.getBooleanValue(account, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI));

            pUploadOnWifiCheckbox.setOnPreferenceClickListener(preference -> {
                arbitraryDataProvider.storeOrUpdateKeyValue(account.name, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI,
                        String.valueOf(pUploadOnWifiCheckbox.isChecked()));

                return true;
            });

            Preference pSyncedFolder = findPreference("synced_folders_configure_folders");
            if (pSyncedFolder != null) {
                if (getResources().getBoolean(R.bool.syncedFolder_light)
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pSyncedFolder.setOnPreferenceClickListener(preference -> {
                        Intent syncedFoldersIntent = new Intent(getApplicationContext(), SyncedFoldersActivity.class);
                        syncedFoldersIntent.putExtra(SyncedFoldersActivity.EXTRA_SHOW_SIDEBAR, false);
                        startActivity(syncedFoldersIntent);
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
        } else if (LOCK_DEVICE_CREDENTIALS.equals(lock)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !DeviceCredentialUtils.areCredentialsAvailable(getApplicationContext())) {
                DisplayUtils.showSnackMessage(this, R.string.prefs_lock_device_credentials_not_setup);
            } else {
                DisplayUtils.showSnackMessage(this, R.string.prefs_lock_device_credentials_enabled);
                this.lock.setValue(LOCK_DEVICE_CREDENTIALS);
                this.lock.setSummary(this.lock.getEntry());
            }
        }
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

    private void setupGeneralCategory(int accentColor) {
        PreferenceCategory preferenceCategoryGeneral = (PreferenceCategory) findPreference("general");
        preferenceCategoryGeneral.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_category_general),
                accentColor));

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

                StorageMigration storageMigration = new StorageMigration(this, storagePath, newPath);
                storageMigration.setStorageMigrationProgressListener(this);
                storageMigration.migrate();

                return false;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        ActionBar actionBar = getDelegate().getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            ThemeUtils.setColoredTitle(actionBar, getString(R.string.actionbar_settings), this);
            actionBar.setBackgroundDrawable(new ColorDrawable(ThemeUtils.primaryColor(this)));

            Drawable backArrow = getResources().getDrawable(R.drawable.ic_arrow_back);
            actionBar.setHomeAsUpIndicator(ThemeUtils.tintDrawable(backArrow, ThemeUtils.fontColor(this)));
        }

        Window window = getWindow();
        if (window != null) {
            window.getDecorView().setBackgroundDrawable(new ColorDrawable(ResourcesCompat
                    .getColor(getResources(), R.color.background_color, null)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(ThemeUtils.primaryColor(this));
            }

            // For adding content description tag to a title field in the action bar
            int actionBarTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
            View actionBarTitle = window.getDecorView().findViewById(actionBarTitleId);

            if (actionBarTitle != null) {
                actionBarTitle.setContentDescription(getString(R.string.actionbar_settings));
            }
        }
    }

    private void launchDavDroidLogin() {
        Intent davDroidLoginIntent = new Intent();
        davDroidLoginIntent.setClassName("at.bitfire.davdroid", "at.bitfire.davdroid.ui.setup.LoginActivity");
        if (getPackageManager().resolveActivity(davDroidLoginIntent, 0) != null) {
            // arguments
            if (serverBaseUri != null) {
                davDroidLoginIntent.putExtra("url", serverBaseUri.toString() + DAV_PATH);
            }
            davDroidLoginIntent.putExtra("username", UserAccountManager.getUsername(account));
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
                DisplayUtils.startIntentIfAppAvailable(downloadIntent, this, R.string.no_browser_available);

                DisplayUtils.showSnackMessage(this, R.string.prefs_calendar_contacts_no_store_error);
            }
        }
    }

    private void setupBaseUri() {
        // retrieve and set user's base URI
        Thread t = new Thread(() -> {
            try {
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, MainApp.getAppContext());
                serverBaseUri = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount,
                        getApplicationContext()).getBaseUri();
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
                lock.setValue(LOCK_PASSCODE);
                lock.setSummary(lock.getEntry());
                DisplayUtils.showSnackMessage(this, R.string.pass_code_stored);
            }
        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {
                lock.setValue(LOCK_NONE);
                lock.setSummary(lock.getEntry());

                DisplayUtils.showSnackMessage(this, R.string.pass_code_removed);
                if (!LOCK_NONE.equals(pendingLock)) {
                    enableLock(pendingLock);
                }
            }
        } else if (requestCode == ACTION_REQUEST_CODE_DAVDROID_SETUP && resultCode == RESULT_OK) {
            DisplayUtils.showSnackMessage(this, R.string.prefs_calendar_contacts_sync_setup_successful);
        } else if (requestCode == ACTION_CONFIRM_DEVICE_CREDENTIALS && resultCode == RESULT_OK &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                data.getIntExtra(RequestCredentialsActivity.KEY_CHECK_RESULT,
                        RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE) ==
                        RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE) {
            lock.setValue(LOCK_NONE);
            lock.setSummary(lock.getEntry());
            DisplayUtils.showSnackMessage(this, R.string.credentials_disabled);
            if (!LOCK_NONE.equals(pendingLock)) {
                enableLock(pendingLock);
            }
        } else if (requestCode == PassCodeManager.PASSCODE_ACTIVITY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (data == null) {
                DisplayUtils.showSnackMessage(this, "Error retrieving mnemonic!");
            } else {
                if (data.getIntExtra(RequestCredentialsActivity.KEY_CHECK_RESULT,
                        RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE) ==
                        RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE) {

                    ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
                    String mnemonic = arbitraryDataProvider.getValue(account.name, EncryptionUtils.MNEMONIC);

                    int accentColor = ThemeUtils.primaryAccentColor(this);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FallbackTheming_Dialog);
                    builder.setTitle(ThemeUtils.getColoredTitle(getString(R.string.prefs_e2e_mnemonic), accentColor));
                    builder.setMessage(mnemonic);
                    builder.setPositiveButton(ThemeUtils.getColoredTitle(getString(R.string.common_ok), accentColor),
                        (dialog, which) -> dialog.dismiss());
                    builder.show();
                }
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
                if (findPreference(link.id.toString()) == null) {
                    Preference p = new Preference(this);
                    p.setTitle(link.name);
                    p.setKey(link.id.toString());

                    p.setOnPreferenceClickListener(preference -> {
                        Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, link.name);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, link.url);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, link.id);
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
        FileActivity.showDevSnackbar(this, latestVersion, true);
    }

}
