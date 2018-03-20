/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * Copyright (C) 2015 ownCloud Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android;

import android.Manifest;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.StringRes;
import android.support.multidex.MultiDexApplication;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.WindowManager;

import com.evernote.android.job.JobManager;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datastorage.DataStorageProvider;
import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.jobs.NCJobCreator;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory.Policy;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.Preferences;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.activity.WhatsNewActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.ReceiversHelper;
import com.owncloud.android.utils.SecurityUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP;


/**
 * Main Application of the project
 *
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 */
public class MainApp extends MultiDexApplication {

    private static final String TAG = MainApp.class.getSimpleName();

    private static final String AUTH_ON = "on";

    @SuppressWarnings("unused")
    private static final String POLICY_SINGLE_SESSION_PER_ACCOUNT = "single session per account";
    @SuppressWarnings("unused")
    private static final String POLICY_ALWAYS_NEW_CLIENT = "always new client";

    private static Context mContext;

    private static String storagePath;

    private static boolean mOnlyOnDevice = false;

    private SharedPreferences appPrefs;
    @SuppressWarnings("unused")
    private boolean mBound;

    @SuppressFBWarnings("ST")
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new NCJobCreator());
        MainApp.mContext = getApplicationContext();

        new SecurityUtils();
        DisplayUtils.useCompatVectorIfNeeded();

        if (!getResources().getBoolean(R.bool.analytics_enabled)) {
            AnalyticsUtils.disableAnalytics();
        }

        appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        fixStoragePath();

        MainApp.storagePath = appPrefs.getString(Preferences.PreferenceKeys.STORAGE_PATH,
                getApplicationContext().getFilesDir().getAbsolutePath());

        boolean isSamlAuth = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));

        OwnCloudClientManagerFactory.setUserAgent(getUserAgent());
        OwnCloudClientManagerFactory.setNextcloudUserAgent(getNextcloudUserAgent());
        if (isSamlAuth) {
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT);
        } else {
            OwnCloudClientManagerFactory
                    .setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT_IF_SERVER_SUPPORTS_SERVER_MONITORING);
        }

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();


        if (BuildConfig.DEBUG || getApplicationContext().getResources().getBoolean(R.bool.logger_enabled) ||
                appPrefs.getBoolean(Preferences.PREFERENCE_EXPERT_MODE, false)) {
            // use app writable dir, no permissions needed
            Log_OC.startLogging(getAppContext());
            Log_OC.d("Debug", "start logging");
        }

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                Log_OC.d("Debug", "Failed to disable uri exposure");
            }
        }

        initSyncOperations();
        initContactsBackup();
        notificationChannels();

        // register global protection with pass code
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log_OC.d(activity.getClass().getSimpleName(), "onCreate(Bundle) starting");
                WhatsNewActivity.runIfNeeded(activity);
                PassCodeManager.getPassCodeManager().onActivityCreated(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onStart() starting");
                PassCodeManager.getPassCodeManager().onActivityStarted(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onResume() starting");
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onPause() ending");
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onStop() ending");
                PassCodeManager.getPassCodeManager().onActivityStopped(activity);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log_OC.d(activity.getClass().getSimpleName(), "onSaveInstanceState(Bundle) starting");
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onDestroy() ending");
            }
        });
    }

    public static void initContactsBackup() {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(mContext.getContentResolver());
        Account[] accounts = AccountUtils.getAccounts(mContext);

        for (Account account : accounts) {
            if (arbitraryDataProvider.getBooleanValue(account, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP)) {
                ContactsPreferenceActivity.startContactBackupJob(account);
            }
        }

    }

    @SuppressLint("ApplySharedPref") // commit is done on purpose to write immediately
    private void fixStoragePath() {
        if (!PreferenceManager.getStoragePathFix(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StoragePoint[] storagePoints = DataStorageProvider.getInstance().getAvailableStoragePoints();
                String storagePath = appPrefs.getString(Preferences.PreferenceKeys.STORAGE_PATH, "");
                if (TextUtils.isEmpty(storagePath)) {
                    if (appPrefs.getInt(WhatsNewActivity.KEY_LAST_SEEN_VERSION_CODE, 0) != 0) {
                        // We already used the app, but no storage is set - fix that!
                        appPrefs.edit().putString(Preferences.PreferenceKeys.STORAGE_PATH,
                                Environment.getExternalStorageDirectory().getAbsolutePath()).commit();
                        appPrefs.edit().remove(PreferenceManager.PREF__KEYS_MIGRATION).commit();
                    } else {
                        // find internal storage path that's indexable
                        boolean set = false;
                        for (StoragePoint storagePoint : storagePoints) {
                            if (storagePoint.getStorageType().equals(StoragePoint.StorageType.INTERNAL) &&
                                    storagePoint.getPrivacyType().equals(StoragePoint.PrivacyType.PUBLIC)) {
                                appPrefs.edit().putString(Preferences.PreferenceKeys.STORAGE_PATH,
                                        storagePoint.getPath()).commit();
                                appPrefs.edit().remove(PreferenceManager.PREF__KEYS_MIGRATION).commit();
                                set = true;
                                break;
                            }
                        }

                        if (!set) {
                            for (StoragePoint storagePoint : storagePoints) {
                                if (storagePoint.getPrivacyType().equals(StoragePoint.PrivacyType.PUBLIC)) {
                                    appPrefs.edit().putString(Preferences.PreferenceKeys.STORAGE_PATH,
                                            storagePoint.getPath()).commit();
                                    appPrefs.edit().remove(PreferenceManager.PREF__KEYS_MIGRATION).commit();
                                    set = true;
                                    break;
                                }
                            }

                        }
                    }
                    PreferenceManager.setStoragePathFix(this, true);
                } else {
                    appPrefs.edit().remove(PreferenceManager.PREF__KEYS_MIGRATION).commit();
                    PreferenceManager.setStoragePathFix(this, true);
                }
            } else {
                if (TextUtils.isEmpty(storagePath)) {
                    appPrefs.edit().putString(Preferences.PreferenceKeys.STORAGE_PATH,
                            Environment.getExternalStorageDirectory().getAbsolutePath()).commit();
                }
                appPrefs.edit().remove(PreferenceManager.PREF__KEYS_MIGRATION).commit();
                PreferenceManager.setStoragePathFix(this, true);
            }
        }
    }

    public static void initSyncOperations() {
        updateToAutoUpload();
        cleanOldEntries();
        updateAutoUploadEntries();

        if (getAppContext() != null) {
            if (PermissionUtil.checkSelfPermission(getAppContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                splitOutAutoUploadEntries();
            } else {
                PreferenceManager.setAutoUploadSplitEntries(getAppContext(), true);
            }
        }

        initiateExistingAutoUploadEntries();

        FilesSyncHelper.scheduleFilesSyncIfNeeded(mContext);
        FilesSyncHelper.restartJobsIfNeeded();
        FilesSyncHelper.scheduleOfflineSyncIfNeeded();

        ReceiversHelper.registerNetworkChangeReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ReceiversHelper.registerPowerChangeReceiver();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ReceiversHelper.registerPowerSaveReceiver();
        }
    }

    public static void notificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && getAppContext() != null) {
            Context context = getAppContext();
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD,
                        R.string.notification_channel_download_name,
                        R.string.notification_channel_download_description, context);

                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD,
                        R.string.notification_channel_upload_name,
                        R.string.notification_channel_upload_description, context);

                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_MEDIA,
                        R.string.notification_channel_media_name,
                        R.string.notification_channel_media_description, context);

                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_FILE_SYNC,
                        R.string.notification_channel_file_sync_name,
                        R.string.notification_channel_file_sync_description, context);

                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_FILE_OBSERVER,
                        R.string.notification_channel_file_observer_name, R.string
                                .notification_channel_file_observer_description, context);

                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_PUSH,
                        R.string.notification_channel_push_name, R.string
                                .notification_channel_push_description, context, NotificationManager.IMPORTANCE_DEFAULT);
            } else {
                Log_OC.e(TAG, "Notification manager is null");
            }
        }
    }

    private static void createChannel(NotificationManager notificationManager,
                                      String channelId, int channelName,
                                      int channelDescription, Context context) {
        createChannel(notificationManager, channelId, channelName, channelDescription, context,
                NotificationManager.IMPORTANCE_LOW);
    }

    private static void createChannel(NotificationManager notificationManager,
                                      String channelId, int channelName,
                                      int channelDescription, Context context, int importance) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                && getAppContext() != null
                && notificationManager.getNotificationChannel(channelId) == null) {
            CharSequence name = context.getString(channelName);
            String description = context.getString(channelDescription);
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);

            channel.setDescription(description);
            channel.enableLights(false);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
    }
    

    public static Context getAppContext() {
        return MainApp.mContext;
    }

    public static void setAppContext(Context context) {
        MainApp.mContext = context;
    }

    public static String getStoragePath() {
        return MainApp.storagePath;
    }

    public static void setStoragePath(String path) {
        MainApp.storagePath = path;
    }

    // Methods to obtain Strings referring app_name 
    //   From AccountAuthenticator 
    //   public static final String ACCOUNT_TYPE = "owncloud";    
    public static String getAccountType() {
        return getAppContext().getResources().getString(R.string.account_type);
    }

    // Non gradle build systems do not provide BuildConfig.VERSION_CODE
    // so we must fallback to this method :(
    public static int getVersionCode() {
        try {
            String thisPackageName = getAppContext().getPackageName();
            return getAppContext().getPackageManager().getPackageInfo(thisPackageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    // Non gradle build systems do not provide BuildConfig.VERSION_CODE
    // so we must fallback to this method :(
    public static String getVersionName() {
        try {
            String thisPackageName = getAppContext().getPackageName();
            return getAppContext().getPackageManager().getPackageInfo(thisPackageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    //  From AccountAuthenticator
    //  public static final String AUTHORITY = "org.owncloud";
    public static String getAuthority() {
        return getAppContext().getResources().getString(R.string.authority);
    }

    //  From AccountAuthenticator
    //  public static final String AUTH_TOKEN_TYPE = "org.owncloud";
    public static String getAuthTokenType() {
        return getAppContext().getResources().getString(R.string.authority);
    }

    //  From ProviderMeta 
    //  public static final String DB_FILE = "owncloud.db";
    public static String getDBFile() {
        return getAppContext().getResources().getString(R.string.db_file);
    }

    //  From ProviderMeta
    //  private final String mDatabaseName = "ownCloud";
    public static String getDBName() {
        return getAppContext().getResources().getString(R.string.db_name);
    }

    /**
     * name of data_folder, e.g., "owncloud"
     */
    public static String getDataFolder() {
        return getAppContext().getResources().getString(R.string.data_folder);
    }

    // log_name
    public static String getLogName() {
        return getAppContext().getResources().getString(R.string.log_name);
    }

    public static void showOnlyFilesOnDevice(boolean state) {
        mOnlyOnDevice = state;
    }

    public static boolean isOnlyOnDevice() {
        return mOnlyOnDevice;
    }

    public static String getUserAgent() {
        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        return getUserAgent(R.string.user_agent);
    }

    public static String getNextcloudUserAgent() {
        // Mozilla/5.0 (Android) Nextcloud-android/2.1.0
        return getUserAgent(R.string.nextcloud_user_agent);
    }

    // user agent
    private static String getUserAgent(@StringRes int agent) {
        String appString = getAppContext().getResources().getString(agent);
        String packageName = getAppContext().getPackageName();
        String version = "";

        PackageInfo pInfo = null;
        try {
            pInfo = getAppContext().getPackageManager().getPackageInfo(packageName, 0);
            if (pInfo != null) {
                version = pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log_OC.e(TAG, "Trying to get packageName", e.getCause());
        }

        return String.format(appString, version);
    }

    private static void updateToAutoUpload() {
            Context context = getAppContext();
            if (PreferenceManager.instantPictureUploadEnabled(context) ||
                            PreferenceManager.instantPictureUploadEnabled(context)) {

                // remove legacy shared preferences
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.remove("instant_uploading")
                        .remove("instant_video_uploading")
                        .remove("instant_upload_path")
                        .remove("instant_upload_path_use_subfolders")
                        .remove("instant_upload_on_wifi")
                        .remove("instant_upload_on_charging")
                        .remove("instant_video_upload_path")
                        .remove("instant_video_upload_path_use_subfolders")
                        .remove("instant_video_upload_on_wifi")
                        .remove("instant_video_uploading")
                        .remove("instant_video_upload_on_charging")
                        .remove("prefs_instant_behaviour").apply();

                // show info pop-up
                try {
                    new AlertDialog.Builder(context, R.style.Theme_ownCloud_Dialog)
                            .setTitle(R.string.drawer_synced_folders)
                            .setMessage(R.string.synced_folders_new_info)
                            .setPositiveButton(R.string.drawer_open, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // show Auto Upload
                                    Intent folderSyncIntent = new Intent(context,
                                            SyncedFoldersActivity.class);
                                    dialog.dismiss();
                                    context.startActivity(folderSyncIntent);
                                }
                            })
                            .setNegativeButton(R.string.drawer_close, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.nav_synced_folders)
                            .show();
                } catch (WindowManager.BadTokenException e) {
                    Log_OC.i(TAG, "Error showing Auto Upload Update dialog, so skipping it: " + e.getMessage());
                }
            }
    }

    private static void updateAutoUploadEntries() {
        // updates entries to reflect their true paths
        Context context = getAppContext();
        if (!PreferenceManager.getAutoUploadPathsUpdate(context)) {
            SyncedFolderProvider syncedFolderProvider =
                    new SyncedFolderProvider(MainApp.getAppContext().getContentResolver());
            syncedFolderProvider.updateAutoUploadPaths(mContext);
        }
    }

    private static void splitOutAutoUploadEntries() {
        Context context = getAppContext();
        if (!PreferenceManager.getAutoUploadSplitEntries(context)) {
            // magic to split out existing synced folders in two when needed
            // otherwise, we migrate them to their proper type (image or video)
            Log_OC.i(TAG, "Migrate synced_folders records for image/video split");
            ContentResolver contentResolver = context.getContentResolver();

            SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

            final List<MediaFolder> imageMediaFolders = MediaProvider.getImageFolders(contentResolver, 1, null);
            final List<MediaFolder> videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 1, null);

            ArrayList<Long> idsToDelete = new ArrayList<>();
            List<SyncedFolder> syncedFolders = syncedFolderProvider.getSyncedFolders();
            long primaryKey;
            SyncedFolder newSyncedFolder;
            for (SyncedFolder syncedFolder : syncedFolders) {
                idsToDelete.add(syncedFolder.getId());
                Log_OC.i(TAG, "Migration check for synced_folders record: "
                        + syncedFolder.getId() + " - " + syncedFolder.getLocalPath());

                for (int i = 0; i < imageMediaFolders.size(); i++) {
                    if (imageMediaFolders.get(i).absolutePath.equals(syncedFolder.getLocalPath())) {
                        newSyncedFolder = (SyncedFolder) syncedFolder.clone();
                        newSyncedFolder.setType(MediaFolderType.IMAGE);
                        primaryKey = syncedFolderProvider.storeSyncedFolder(newSyncedFolder);
                        Log_OC.i(TAG, "Migrated image synced_folders record: "
                                + primaryKey + " - " + newSyncedFolder.getLocalPath());
                        break;
                    }
                }

                for (int j = 0; j < videoMediaFolders.size(); j++) {
                    if (videoMediaFolders.get(j).absolutePath.equals(syncedFolder.getLocalPath())) {
                        newSyncedFolder = (SyncedFolder) syncedFolder.clone();
                        newSyncedFolder.setType(MediaFolderType.VIDEO);
                        primaryKey = syncedFolderProvider.storeSyncedFolder(newSyncedFolder);
                        Log_OC.i(TAG, "Migrated video synced_folders record: "
                                + primaryKey + " - " + newSyncedFolder.getLocalPath());
                        break;
                    }
                }
            }

            for (long id : idsToDelete) {
                Log_OC.i(TAG, "Removing legacy synced_folders record: " + id);
                syncedFolderProvider.deleteSyncedFolder(id);
            }

            PreferenceManager.setAutoUploadSplitEntries(context, true);
        }
    }

    private static void initiateExistingAutoUploadEntries() {
        new Thread(() -> {
            if (!PreferenceManager.getAutoUploadInit(getAppContext())) {
                SyncedFolderProvider syncedFolderProvider =
                        new SyncedFolderProvider(MainApp.getAppContext().getContentResolver());

                for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
                    if (syncedFolder.isEnabled()) {
                        FilesSyncHelper.insertAllDBEntriesForSyncedFolder(syncedFolder);
                    }
                }

                PreferenceManager.setAutoUploadInit(getAppContext(), true);
            }

        }).start();
    }

    private static void cleanOldEntries() {
        // previous versions of application created broken entries in the SyncedFolderProvider
        // database, and this cleans all that and leaves 1 (newest) entry per synced folder

        Context context = getAppContext();

        if (!PreferenceManager.getLegacyClean(context)) {
            SyncedFolderProvider syncedFolderProvider =
                    new SyncedFolderProvider(context.getContentResolver());

            List<SyncedFolder> syncedFolderList = syncedFolderProvider.getSyncedFolders();
            Map<Pair<String, String>, Long> syncedFolders = new HashMap<>();
            ArrayList<Long> ids = new ArrayList<>();
            for (SyncedFolder syncedFolder : syncedFolderList) {
                Pair<String, String> checkPair = new Pair<>(syncedFolder.getAccount(), syncedFolder.getLocalPath());
                if (syncedFolders.containsKey(checkPair)) {
                    if (syncedFolder.getId() > syncedFolders.get(checkPair)) {
                        syncedFolders.put(checkPair, syncedFolder.getId());
                    }
                } else {
                    syncedFolders.put(checkPair, syncedFolder.getId());
                }
            }

            ids.addAll(syncedFolders.values());

            if (ids.size() > 0) {
                syncedFolderProvider.deleteSyncedFoldersNotInList(mContext, ids);
            } else {
                PreferenceManager.setLegacyClean(context, true);
            }
        }
    }
}
