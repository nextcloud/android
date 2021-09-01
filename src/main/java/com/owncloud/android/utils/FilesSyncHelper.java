/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;

import java.io.File;
import java.io.IOException;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;

/**
 * Various utilities that make auto upload tick
 */
public final class FilesSyncHelper {
    public static final String TAG = "FileSyncHelper";

    public static final String GLOBAL = "global";

    public static final int ContentSyncJobId = 315;

    private FilesSyncHelper() {
        // utility class -> private constructor
    }

    private static void insertAllDBEntriesForSyncedFolder(SyncedFolder syncedFolder) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        final long enabledTimestampMs = syncedFolder.getEnabledTimestampMs();

        if (syncedFolder.isEnabled() && (syncedFolder.isExisting() || enabledTimestampMs >= 0)) {
            MediaFolderType mediaType = syncedFolder.getType();
            if (mediaType == MediaFolderType.IMAGE) {
                FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    , syncedFolder);
                FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                    syncedFolder);
            } else if (mediaType == MediaFolderType.VIDEO) {
                FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                                                    syncedFolder);
                FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                    syncedFolder);
            } else {
                try {
                    FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
                    Path path = Paths.get(syncedFolder.getLocalPath());

                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                            File file = path.toFile();
                            if (syncedFolder.isExisting() || attrs.lastModifiedTime().toMillis() >= enabledTimestampMs) {
                                filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                                                              attrs.lastModifiedTime().toMillis(),
                                                                              file.isDirectory(), syncedFolder);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    Log_OC.e(TAG, "Something went wrong while indexing files for auto upload", e);
                }
            }
        }
    }

    public static void insertAllDBEntries(AppPreferences preferences,
                                          Clock clock,
                                          boolean skipCustom) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver, preferences, clock);

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled() && (!skipCustom || syncedFolder.getType() != MediaFolderType.CUSTOM)) {
                insertAllDBEntriesForSyncedFolder(syncedFolder);
            }
        }
    }

    private static void insertContentIntoDB(Uri uri, SyncedFolder syncedFolder) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor;
        int column_index_data;
        int column_index_date_modified;

        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);

        String contentPath;
        boolean isFolder;

        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_MODIFIED};

        String path = syncedFolder.getLocalPath();
        if (!path.endsWith(PATH_SEPARATOR)) {
            path = path + PATH_SEPARATOR;
        }
        path = path + "%";

        long enabledTimestampMs = syncedFolder.getEnabledTimestampMs();

        cursor = context.getContentResolver().query(uri, projection, MediaStore.MediaColumns.DATA + " LIKE ?",
                                                    new String[]{path}, null);

        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_date_modified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            while (cursor.moveToNext()) {
                contentPath = cursor.getString(column_index_data);
                isFolder = new File(contentPath).isDirectory();
                if (syncedFolder.isExisting() || cursor.getLong(column_index_date_modified) >= enabledTimestampMs / 1000.0) {
                    filesystemDataProvider.storeOrUpdateFileValue(contentPath,
                                                                  cursor.getLong(column_index_date_modified), isFolder,
                                                                  syncedFolder);
                }
            }
            cursor.close();
        }
    }

    public static void restartJobsIfNeeded(final UploadsStorageManager uploadsStorageManager,
                                           final UserAccountManager accountManager,
                                           final ConnectivityService connectivityService,
                                           final PowerManagementService powerManagementService) {
        final Context context = MainApp.getAppContext();

        boolean accountExists;

        boolean whileChargingOnly = true;
        boolean useWifiOnly = true;

        OCUpload[] failedUploads = uploadsStorageManager.getFailedUploads();

        for (OCUpload failedUpload : failedUploads) {
            accountExists = false;
            if(!failedUpload.isWhileChargingOnly()){
                whileChargingOnly = false;
            }
            if(!failedUpload.isUseWifiOnly())
            {
                useWifiOnly = false;
            }

            // check if accounts still exists
            for (Account account : accountManager.getAccounts()) {
                if (account.name.equals(failedUpload.getAccountName())) {
                    accountExists = true;
                    break;
                }
            }

            if (!accountExists) {
                uploadsStorageManager.removeUpload(failedUpload);
            }
        }

        failedUploads = uploadsStorageManager.getFailedUploads();
        if(failedUploads.length == 0)
        {
            //nothing to do
            return;
        }

        if(whileChargingOnly){
            final BatteryStatus batteryStatus = powerManagementService.getBattery();
            final boolean charging = batteryStatus.isCharging() || batteryStatus.isFull();
            if(!charging){
                //all uploads requires charging
                return;
            }
        }

        if (useWifiOnly && !connectivityService.getConnectivity().isWifi()){
            //all uploads requires wifi
            return;
        }

        new Thread(() -> {
            if (connectivityService.getConnectivity().isConnected() && !connectivityService.isInternetWalled()) {
                FileUploader.retryFailedUploads(
                    context,
                    null,
                    uploadsStorageManager,
                    connectivityService,
                    accountManager,
                    powerManagementService,
                    null
                );
            }
        }).start();
    }

    public static void scheduleFilesSyncIfNeeded(Context context, BackgroundJobManager jobManager) {
        jobManager.schedulePeriodicFilesSyncJob();
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobManager.scheduleContentObserverJob();
        }
    }
}

