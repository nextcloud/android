/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Chris Narkiewicz
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.WindowManager;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.di.ActivityInjector;
import com.nextcloud.client.di.DaggerAppComponent;
import com.nextcloud.client.errorhandling.ExceptionHandler;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.logger.LegacyLoggerAdapter;
import com.nextcloud.client.logger.Logger;
import com.nextcloud.client.migrations.MigrationsManager;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.onboarding.OnboardingService;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.client.preferences.DarkMode;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datastorage.DataStorageProvider;
import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.jobs.MediaFoldersDetectionJob;
import com.owncloud.android.jobs.NCJobCreator;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.ReceiversHelper;
import com.owncloud.android.utils.SecurityUtils;

import org.conscrypt.Conscrypt;
import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.util.Pair;
import androidx.multidex.MultiDexApplication;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import de.cotech.hw.SecurityKeyManager;
import de.cotech.hw.SecurityKeyManagerConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP;


/**
 * Main Application of the project
 *
 * Contains methods to build the "static" strings. These strings were before constants in different classes
 */
public class MainApp extends MultiDexApplication implements HasAndroidInjector {

    public static final OwnCloudVersion OUTDATED_SERVER_VERSION = OwnCloudVersion.nextcloud_15;
    public static final OwnCloudVersion MINIMUM_SUPPORTED_SERVER_VERSION = OwnCloudVersion.nextcloud_13;

    private static final String TAG = MainApp.class.getSimpleName();
    public static final String DOT = ".";

    private static Context mContext;

    private static String storagePath;

    private static boolean mOnlyOnDevice;

    @Inject
    protected AppPreferences preferences;

    @Inject
    protected DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    @Inject
    protected UserAccountManager accountManager;

    @Inject
    protected UploadsStorageManager uploadsStorageManager;

    @Inject
    protected OnboardingService onboarding;

    @Inject
    ConnectivityService connectivityService;

    @Inject PowerManagementService powerManagementService;

    @Inject
    Logger logger;

    @Inject
    AppInfo appInfo;

    @Inject
    BackgroundJobManager backgroundJobManager;

    @Inject
    Clock clock;

    @Inject
    EventBus eventBus;

    @Inject
    MigrationsManager migrationsManager;

    private PassCodeManager passCodeManager;

    @SuppressWarnings("unused")
    private boolean mBound;

    /**
     * Temporary hack
     */
    private static void initGlobalContext(Context context) {
        mContext = context;
    }

    /**
     * Temporary getter replacing Dagger DI
     * TODO: remove when cleaning DI in NContentObserverJob
     */
    public AppPreferences getPreferences() {
        return preferences;
    }

    /**
     * Temporary getter replacing Dagger DI
     * TODO: remove when cleaning DI in NContentObserverJob
     */
    public PowerManagementService getPowerManagementService() {
        return powerManagementService;
    }

    /**
     * Temporary getter enabling intermediate refactoring.
     * TODO: remove when FileSyncHelper is refactored/removed
     */
    public BackgroundJobManager getBackgroundJobManager() {
        return backgroundJobManager;
    }

    private String getAppProcessName() {
        String processName = "";
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            final int ownPid = android.os.Process.myPid();
            final List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo info : processes) {
                    if (info.pid == ownPid) {
                        processName = info.processName;
                        break;
                    }
                }
            }
        } else {
            processName = Application.getProcessName();
        }
        return processName;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        initGlobalContext(this);
        DaggerAppComponent.builder()
            .application(this)
            .build()
            .inject(this);

        // we don't want to handle crashes occurring inside crash reporter activity/process;
        // let the platform deal with those
        final boolean isCrashReportingProcess = getAppProcessName().endsWith(":crash");
        final boolean useExceptionHandler = !appInfo.isDebugBuild();

        if (!isCrashReportingProcess && useExceptionHandler) {
            Thread.UncaughtExceptionHandler defaultPlatformHandler = Thread.getDefaultUncaughtExceptionHandler();
            final ExceptionHandler crashReporter = new ExceptionHandler(this,
                                                                        defaultPlatformHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashReporter);
        }
    }

    @SuppressFBWarnings("ST")
    @Override
    public void onCreate() {
        setAppTheme(preferences.getDarkThemeMode());
        super.onCreate();

        insertConscrypt();

        initSecurityKeyManager();

        registerActivityLifecycleCallbacks(new ActivityInjector());

        int startedMigrationsCount = migrationsManager.startMigration();
        logger.i(TAG, String.format(Locale.US, "Started %d migrations", startedMigrationsCount));

        JobManager.create(this).addJobCreator(
            new NCJobCreator(
                getApplicationContext(),
                accountManager,
                preferences,
                uploadsStorageManager,
                connectivityService,
                powerManagementService,
                clock,
                eventBus
            )
        );

        new SecurityUtils();
        DisplayUtils.useCompatVectorIfNeeded();

        fixStoragePath();
        passCodeManager = new PassCodeManager(preferences);

        MainApp.storagePath = preferences.getStoragePath(getApplicationContext().getFilesDir().getAbsolutePath());

        OwnCloudClientManagerFactory.setUserAgent(getUserAgent());

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();


        if (BuildConfig.DEBUG || getApplicationContext().getResources().getBoolean(R.bool.logger_enabled)) {
            // use app writable dir, no permissions needed
            Log_OC.setLoggerImplementation(new LegacyLoggerAdapter(logger));
            Log_OC.d("Debug", "start logging");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                Log_OC.d("Debug", "Failed to disable uri exposure");
            }
        }
        initSyncOperations(uploadsStorageManager,
                           accountManager,
                           connectivityService,
                           powerManagementService,
                           backgroundJobManager,
                           clock);
        initContactsBackup(accountManager);
        notificationChannels();


        new JobRequest.Builder(MediaFoldersDetectionJob.TAG)
            .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
            .setUpdateCurrent(true)
            .build()
            .schedule();

        new JobRequest.Builder(MediaFoldersDetectionJob.TAG)
            .startNow()
            .setUpdateCurrent(false)
            .build()
            .schedule();

        registerGlobalPassCodeProtection();
    }

    private void registerGlobalPassCodeProtection() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log_OC.d(activity.getClass().getSimpleName(), "onCreate(Bundle) starting");
                onboarding.launchActivityIfNeeded(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onStart() starting");
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onResume() starting");
                passCodeManager.onActivityStarted(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onPause() ending");
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onStop() ending");
                passCodeManager.onActivityStopped(activity);
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

    private void initSecurityKeyManager() {
        SecurityKeyManager securityKeyManager = SecurityKeyManager.getInstance();
        SecurityKeyManagerConfig config = new SecurityKeyManagerConfig.Builder()
            .setEnableDebugLogging(BuildConfig.DEBUG)
            .build();
        securityKeyManager.init(this, config);
    }

    public static void initContactsBackup(UserAccountManager accountManager) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(mContext.getContentResolver());
        Account[] accounts = accountManager.getAccounts();

        for (Account account : accounts) {
            if (arbitraryDataProvider.getBooleanValue(account, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP)) {
                ContactsPreferenceActivity.startContactBackupJob(account);
            }
        }
    }

    private void insertConscrypt() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);

        try {
            Conscrypt.Version version = Conscrypt.version();
            Log_OC.i(TAG, "Using Conscrypt/"
                + version.major()
                + DOT
                + version.minor()
                + DOT + version.patch()
                + " for TLS");
            SSLEngine engine = SSLContext.getDefault().createSSLEngine();
            Log_OC.i(TAG, "Enabled protocols: " + Arrays.toString(engine.getEnabledProtocols()) + " }");
            Log_OC.i(TAG, "Enabled ciphers: " + Arrays.toString(engine.getEnabledCipherSuites()) + " }");
        } catch (NoSuchAlgorithmException e) {
            Log_OC.e(TAG, e.getMessage());
        }
    }

    @SuppressLint("ApplySharedPref") // commit is done on purpose to write immediately
    private void fixStoragePath() {
        if (!preferences.isStoragePathFixEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StoragePoint[] storagePoints = DataStorageProvider.getInstance().getAvailableStoragePoints();
                String storagePath = preferences.getStoragePath("");

                if (TextUtils.isEmpty(storagePath)) {
                    if (preferences.getLastSeenVersionCode() != 0) {
                        // We already used the app, but no storage is set - fix that!
                        preferences.setStoragePath(Environment.getExternalStorageDirectory().getAbsolutePath());
                        preferences.removeKeysMigrationPreference();
                    } else {
                        // find internal storage path that's indexable
                        boolean set = false;
                        for (StoragePoint storagePoint : storagePoints) {
                            if (storagePoint.getStorageType() == StoragePoint.StorageType.INTERNAL &&
                                storagePoint.getPrivacyType() == StoragePoint.PrivacyType.PUBLIC) {
                                preferences.setStoragePath(storagePoint.getPath());
                                preferences.removeKeysMigrationPreference();
                                set = true;
                                break;
                            }
                        }

                        if (!set) {
                            for (StoragePoint storagePoint : storagePoints) {
                                if (storagePoint.getPrivacyType() == StoragePoint.PrivacyType.PUBLIC) {
                                    preferences.setStoragePath(storagePoint.getPath());
                                    preferences.removeKeysMigrationPreference();
                                    set = true;
                                    break;
                                }
                            }

                        }
                    }
                    preferences.setStoragePathFixEnabled(true);
                } else {
                    preferences.removeKeysMigrationPreference();
                    preferences.setStoragePathFixEnabled(true);
                }
            } else {
                if (TextUtils.isEmpty(storagePath)) {
                    preferences.setStoragePath(Environment.getExternalStorageDirectory().getAbsolutePath());
                }
                preferences.removeKeysMigrationPreference();
                preferences.setStoragePathFixEnabled(true);
            }
        }
    }

    public static void initSyncOperations(
        final UploadsStorageManager uploadsStorageManager,
        final UserAccountManager accountManager,
        final ConnectivityService connectivityService,
        final PowerManagementService powerManagementService,
        final BackgroundJobManager jobManager,
        final Clock clock
    ) {
        updateToAutoUpload();
        cleanOldEntries(clock);
        updateAutoUploadEntries(clock);

        if (getAppContext() != null) {
            if (PermissionUtil.checkSelfPermission(getAppContext(),
                                                   Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                splitOutAutoUploadEntries(clock);
            } else {
                AppPreferences preferences = AppPreferencesImpl.fromContext(getAppContext());
                preferences.setAutoUploadSplitEntriesEnabled(true);
            }
        }

        initiateExistingAutoUploadEntries(clock);

        FilesSyncHelper.scheduleFilesSyncIfNeeded(mContext, jobManager);
        FilesSyncHelper.restartJobsIfNeeded(
            uploadsStorageManager,
            accountManager,
            connectivityService,
            powerManagementService);
        FilesSyncHelper.scheduleOfflineSyncIfNeeded();

        ReceiversHelper.registerNetworkChangeReceiver(uploadsStorageManager,
                                                      accountManager,
                                                      connectivityService,
                                                      powerManagementService);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ReceiversHelper.registerPowerChangeReceiver(uploadsStorageManager,
                                                        accountManager,
                                                        connectivityService,
                                                        powerManagementService);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ReceiversHelper.registerPowerSaveReceiver(uploadsStorageManager,
                                                      accountManager,
                                                      connectivityService,
                                                      powerManagementService);
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

                createChannel(notificationManager, NotificationUtils.NOTIFICATION_CHANNEL_GENERAL, R.string
                                  .notification_channel_general_name, R.string.notification_channel_general_description,
                              context, NotificationManager.IMPORTANCE_DEFAULT);
            } else {
                Log_OC.e(TAG, "Notification manager is null");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
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
    public static String getAccountType(Context context) {
        return context.getResources().getString(R.string.account_type);
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

    public static void showOnlyFilesOnDevice(boolean state) {
        mOnlyOnDevice = state;
    }

    public static boolean isOnlyOnDevice() {
        return mOnlyOnDevice;
    }

    public static String getUserAgent() {
        // Mozilla/5.0 (Android) Nextcloud-android/2.1.0
        return getUserAgent(R.string.nextcloud_user_agent);
    }

    // user agent
    private static String getUserAgent(@StringRes int agent) {
        String appString = getAppContext().getResources().getString(agent);
        String packageName = getAppContext().getPackageName();
        String version = "";

        try {
            PackageInfo pInfo = getAppContext().getPackageManager().getPackageInfo(packageName, 0);
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
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (preferences.instantPictureUploadEnabled() || preferences.instantVideoUploadEnabled()) {
            preferences.removeLegacyPreferences();

            // show info pop-up
            try {
                new AlertDialog.Builder(context, R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.drawer_synced_folders)
                    .setMessage(R.string.synced_folders_new_info)
                    .setPositiveButton(R.string.drawer_open, (dialog, which) -> {
                        // show Auto Upload
                        Intent folderSyncIntent = new Intent(context, SyncedFoldersActivity.class);
                        dialog.dismiss();
                        context.startActivity(folderSyncIntent);
                    })
                    .setNegativeButton(R.string.drawer_close, (dialog, which) -> dialog.dismiss())
                    .setIcon(R.drawable.nav_synced_folders)
                    .show();
            } catch (WindowManager.BadTokenException e) {
                Log_OC.i(TAG, "Error showing Auto Upload Update dialog, so skipping it: " + e.getMessage());
            }
        }
    }

    private static void updateAutoUploadEntries(Clock clock) {
        // updates entries to reflect their true paths
        Context context = getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (!preferences.isAutoUploadPathsUpdateEnabled()) {
            SyncedFolderProvider syncedFolderProvider =
                new SyncedFolderProvider(MainApp.getAppContext().getContentResolver(), preferences, clock);
            syncedFolderProvider.updateAutoUploadPaths(mContext);
        }
    }

    private static void splitOutAutoUploadEntries(Clock clock) {
        Context context = getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (!preferences.isAutoUploadSplitEntriesEnabled()) {
            // magic to split out existing synced folders in two when needed
            // otherwise, we migrate them to their proper type (image or video)
            Log_OC.i(TAG, "Migrate synced_folders records for image/video split");
            ContentResolver contentResolver = context.getContentResolver();

            SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver, preferences, clock);

            final List<MediaFolder> imageMediaFolders = MediaProvider.getImageFolders(contentResolver, 1, null, true);
            final List<MediaFolder> videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 1, null, true);

            ArrayList<Long> idsToDelete = new ArrayList<>();
            List<SyncedFolder> syncedFolders = syncedFolderProvider.getSyncedFolders();
            long primaryKey;
            SyncedFolder newSyncedFolder;
            for (SyncedFolder syncedFolder : syncedFolders) {
                idsToDelete.add(syncedFolder.getId());
                Log_OC.i(TAG, "Migration check for synced_folders record: "
                    + syncedFolder.getId() + " - " + syncedFolder.getLocalPath());

                for (MediaFolder imageMediaFolder : imageMediaFolders) {
                    if (imageMediaFolder.absolutePath.equals(syncedFolder.getLocalPath())) {
                        newSyncedFolder = (SyncedFolder) syncedFolder.clone();
                        newSyncedFolder.setType(MediaFolderType.IMAGE);
                        primaryKey = syncedFolderProvider.storeSyncedFolder(newSyncedFolder);
                        Log_OC.i(TAG, "Migrated image synced_folders record: "
                            + primaryKey + " - " + newSyncedFolder.getLocalPath());
                        break;
                    }
                }

                for (MediaFolder videoMediaFolder : videoMediaFolders) {
                    if (videoMediaFolder.absolutePath.equals(syncedFolder.getLocalPath())) {
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

            preferences.setAutoUploadSplitEntriesEnabled(true);
        }
    }

    private static void initiateExistingAutoUploadEntries(Clock clock) {
        new Thread(() -> {
            AppPreferences preferences = AppPreferencesImpl.fromContext(getAppContext());
            if (!preferences.isAutoUploadInitialized()) {
                SyncedFolderProvider syncedFolderProvider =
                    new SyncedFolderProvider(MainApp.getAppContext().getContentResolver(), preferences, clock);

                for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
                    if (syncedFolder.isEnabled()) {
                        FilesSyncHelper.insertAllDBEntriesForSyncedFolder(syncedFolder, true);
                    }
                }

                preferences.setAutoUploadInit(true);
            }

        }).start();
    }

    private static void cleanOldEntries(Clock clock) {
        // previous versions of application created broken entries in the SyncedFolderProvider
        // database, and this cleans all that and leaves 1 (newest) entry per synced folder

        Context context = getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);

        if (!preferences.isLegacyClean()) {
            SyncedFolderProvider syncedFolderProvider =
                new SyncedFolderProvider(context.getContentResolver(), preferences, clock);

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
                int deletedCount = syncedFolderProvider.deleteSyncedFoldersNotInList(ids);
                if (deletedCount > 0) {
                    preferences.setLegacyClean(true);
                }
            } else {
                preferences.setLegacyClean(true);
            }
        }
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }


    public static void setAppTheme(DarkMode mode) {
        switch (mode) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
