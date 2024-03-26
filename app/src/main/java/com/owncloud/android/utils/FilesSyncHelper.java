/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;

/**
 * Various utilities that make auto upload tick
 */
public final class FilesSyncHelper {
    public static final String TAG = "FileSyncHelper";

    public static final String GLOBAL = "global";

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

                    long startTime = System.nanoTime();
                    // chick check for changes
                    long lastCheck = System.currentTimeMillis();
                    ArrayList<File> changedFiles = new ArrayList<>();
                    FileUtil.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                            File file = path.toFile();
                            if (syncedFolder.isExcludeHidden() && file.isHidden()) {
                                // exclude hidden file or folder
                                return FileVisitResult.CONTINUE;
                            }
                            if (file.lastModified() >= lastCheck){
                                changedFiles.add(file);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            if (syncedFolder.isExcludeHidden() && dir.compareTo(Paths.get(syncedFolder.getLocalPath())) != 0 && dir.toFile().isHidden()) {
                                return null;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    Log_OC.d(TAG,"FILESYNC FINISHED QUICK CHECK FILE "+path+" "+(System.nanoTime() - startTime));
                    startTime = System.nanoTime();
                    FileUtil.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                            File file = path.toFile();
                            if (syncedFolder.isExcludeHidden() && file.isHidden()) {
                                // exclude hidden file or folder
                                return FileVisitResult.CONTINUE;
                            }
                            if (syncedFolder.isExisting() || attrs.lastModifiedTime().toMillis() >= enabledTimestampMs) {
                                // storeOrUpdateFileValue takes a few millisec
                                // -> Rest of this file check takes not even 1 millisec.
                                filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                                                              attrs.lastModifiedTime().toMillis(),
                                                                              file.isDirectory(), syncedFolder);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            if (syncedFolder.isExcludeHidden() && dir.compareTo(Paths.get(syncedFolder.getLocalPath())) != 0 && dir.toFile().isHidden()) {
                                return null;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    Log_OC.d(TAG,"FILESYNC FINISHED LONG CHECK FILE "+path+" "+(System.nanoTime() - startTime));

                } catch (IOException e) {
                    Log_OC.e(TAG, "Something went wrong while indexing files for auto upload", e);
                }
            }
        }
    }

    public static void insertAllDBEntries(SyncedFolderProvider syncedFolderProvider) {
        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            Log_OC.d(TAG,"FILESYNC CHECK FOLDER "+syncedFolder.getLocalPath());
            if (syncedFolder.isEnabled()) {
                insertAllDBEntriesForSyncedFolder(syncedFolder);
            }
        }
    }

    public static void insertChangedEntries(SyncedFolderProvider syncedFolderProvider,
                                            String[] changedFiles) {
        final ContentResolver contentResolver = MainApp.getAppContext().getContentResolver();
        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
        for (String changedFileURI : changedFiles){
            String changedFile = getFileFromURI(changedFileURI);
            for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
                if (syncedFolder.isEnabled() && syncedFolder.containsFile(changedFile)){
                    File file = new File(changedFile);
                    filesystemDataProvider.storeOrUpdateFileValue(changedFile,
                                                                  file.lastModified(),file.isDirectory(),
                                                                  syncedFolder);
                    Log_OC.d(TAG,"FILESYNC ADDED UPLOAD TO DB");
                    break;
                }
            }
        }
    }

    private static String getFileFromURI(String uri){
        final Context context = MainApp.getAppContext();

        Cursor cursor;
        int column_index_data;
        String filePath = null;

        String[] projection = {MediaStore.MediaColumns.DATA};

        cursor = context.getContentResolver().query(Uri.parse(uri), projection, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            filePath = cursor.getString(column_index_data);
            cursor.close();
        }
        return filePath;
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
                Log_OC.d(TAG,"FILESYNC CHECK File "+contentPath);
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
        boolean accountExists;

        boolean whileChargingOnly = true;
        boolean useWifiOnly = true;

        // Make all in progress downloads failed to restart upload worker
        uploadsStorageManager.failInProgressUploads(UploadResult.SERVICE_INTERRUPTED);

        OCUpload[] failedUploads = uploadsStorageManager.getFailedUploads();

        for (OCUpload failedUpload : failedUploads) {
            accountExists = false;
            if (!failedUpload.isWhileChargingOnly()) {
                whileChargingOnly = false;
            }
            if (!failedUpload.isUseWifiOnly()) {
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
        if (failedUploads.length == 0) {
            //nothing to do
            return;
        }

        if (whileChargingOnly) {
            final BatteryStatus batteryStatus = powerManagementService.getBattery();
            final boolean charging = batteryStatus.isCharging() || batteryStatus.isFull();
            if (!charging) {
                //all uploads requires charging
                return;
            }
        }

        if (useWifiOnly && !connectivityService.getConnectivity().isWifi()) {
            //all uploads requires wifi
            return;
        }

        new Thread(() -> {
            if (connectivityService.getConnectivity().isConnected()) {
                FileUploadHelper.Companion.instance().retryFailedUploads(
                    uploadsStorageManager,
                    connectivityService,
                    accountManager,
                    powerManagementService);
            }
        }).start();
    }

    public static void scheduleFilesSyncIfNeeded(Context context, BackgroundJobManager jobManager) {
        jobManager.schedulePeriodicFilesSyncJob();
        if (context != null) {
            jobManager.scheduleContentObserverJob();
        }
    }
}

