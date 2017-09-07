/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * Copyright (C) 2015 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android;

import android.Manifest;
import android.app.Activity;
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
import android.support.multidex.MultiDexApplication;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import com.evernote.android.job.JobManager;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.jobs.NCJobCreator;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory.Policy;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.Preferences;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.activity.WhatsNewActivity;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.ReceiversHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


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

    @SuppressWarnings("unused")
    private boolean mBound;

    @SuppressFBWarnings("ST")
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new NCJobCreator());
        MainApp.mContext = getApplicationContext();

        if (!getResources().getBoolean(R.bool.analytics_enabled)) {
            AnalyticsUtils.disableAnalytics();
        }

        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        MainApp.storagePath = appPrefs.getString(Preferences.PreferenceKeys.STORAGE_PATH, Environment.
                getExternalStorageDirectory().getAbsolutePath());

        boolean isSamlAuth = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));

        OwnCloudClientManagerFactory.setUserAgent(getUserAgent());
        if (isSamlAuth) {
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT);
        } else {
            OwnCloudClientManagerFactory
                    .setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT_IF_SERVER_SUPPORTS_SERVER_MONITORING);
        }

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();

        if (BuildConfig.DEBUG) {
            // use app writable dir, no permissions needed
            Log_OC.startLogging(getAppContext());
            Log_OC.d("Debug", "start logging");
        }

        initAutoUpload();

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

    public static void initAutoUpload() {
        updateToAutoUpload();
        cleanOldEntries();
        updateAutoUploadEntries();

        if (getAppContext() != null) {
            if (PermissionUtil.checkSelfPermission(getAppContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                splitOutAutoUploadEntries();
            } else {
                PreferenceManager.setAutoUploadSplitEntries(getAppContext(), true);
            }
        }

        initiateExistingAutoUploadEntries();

        FilesSyncHelper.scheduleFilesSyncIfNeeded(mContext);
        FilesSyncHelper.restartJobsIfNeeded();

        ReceiversHelper.registerNetworkChangeReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ReceiversHelper.registerPowerChangeReceiver();
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

    // user agent
    public static String getUserAgent() {
        String appString = getAppContext().getResources().getString(R.string.user_agent);
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

        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        String userAgent = String.format(appString, version);

        return userAgent;
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
            final List<MediaFolder> videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 1);

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
