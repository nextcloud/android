/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
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
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.jobs.AutoUploadJob;
import com.owncloud.android.operations.UploadFileOperation;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class FilesSyncHelper {
    public static final String TAG = "FileSyncHelper";

    public static void insertAllDBEntriesForSyncedFolder(SyncedFolder syncedFolder) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);

        String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + syncedFolder.getId();
        boolean dryRun = TextUtils.isEmpty(arbitraryDataProvider.getValue
                ("global", syncedFolderInitiatedKey));

        if (MediaFolder.IMAGE == syncedFolder.getType()) {
            FilesSyncHelper.insertContentIntoDB(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    , dryRun, syncedFolder);
            FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, dryRun,
                    syncedFolder);

            if (dryRun) {
                arbitraryDataProvider.storeOrUpdateKeyValue("global", syncedFolderInitiatedKey,
                        "1");
            }
        } else if (MediaFolder.VIDEO == syncedFolder.getType()) {
            FilesSyncHelper.insertContentIntoDB(android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                    dryRun, syncedFolder);
            FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, dryRun,
                    syncedFolder);

            if (dryRun) {
                arbitraryDataProvider.storeOrUpdateKeyValue("global", syncedFolderInitiatedKey,
                        "1");
            }

        } else {
            try {

                FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
                Path path = Paths.get(syncedFolder.getLocalPath());

                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                        File file = path.toFile();
                        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
                        FileLock lock = channel.lock();
                        try {
                            lock = channel.tryLock();
                            filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                    attrs.lastModifiedTime().toMillis(), file.isDirectory(), syncedFolder, dryRun);
                        } catch (OverlappingFileLockException e) {
                            filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                    attrs.lastModifiedTime().toMillis(), file.isDirectory(), syncedFolder, dryRun);
                        } finally {
                            lock.release();
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });

                if (dryRun) {
                    arbitraryDataProvider.storeOrUpdateKeyValue("global", syncedFolderInitiatedKey,
                            "1");
                }

            } catch (IOException e) {
                Log.d(TAG, "Something went wrong while indexing files for auto upload");
            }
        }
    }

    public static void insertAllDBEntries() {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                insertAllDBEntriesForSyncedFolder(syncedFolder);
            }
        }
    }

    private static void insertContentIntoDB(Uri uri, boolean dryRun, SyncedFolder syncedFolder) {
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
        if (!path.endsWith("/")) {
            path = path + "/%";
        } else {
            path = path + "%";
        }

        cursor = context.getContentResolver().query(uri, projection, MediaStore.MediaColumns.DATA + " LIKE ?",
                new String[]{path}, null);

        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_date_modified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            while (cursor.moveToNext()) {
                contentPath = cursor.getString(column_index_data);
                isFolder = new File(contentPath).isDirectory();
                filesystemDataProvider.storeOrUpdateFileValue(cursor.getString(column_index_data),
                        cursor.getLong(column_index_date_modified), isFolder, syncedFolder, dryRun);
            }
            cursor.close();
        }
    }

    public static void restartJobsIfNeeded() {
        final Context context = MainApp.getAppContext();
        boolean restartedInCurrentIteration;

        FileUploader.UploadRequester uploadRequester = new FileUploader.UploadRequester();

        boolean accountExists;
        boolean fileExists;

        for (JobRequest jobRequest : JobManager.instance().getAllJobRequestsForTag(AutoUploadJob.TAG)) {
            restartedInCurrentIteration = false;

            accountExists = false;
            fileExists = new File(jobRequest.getExtras().getString(AutoUploadJob.LOCAL_PATH, "")).exists();

            // check if accounts still exists
            for (Account account : AccountUtils.getAccounts(context)) {
                if (account.name.equals(jobRequest.getExtras().getString(AutoUploadJob.ACCOUNT, ""))) {
                    accountExists = true;
                    break;
                }
            }

            if (accountExists && fileExists) {
                // Handle case of charging
                if (jobRequest.requiresCharging() && Device.isCharging(context)) {
                    if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.CONNECTED) &&
                            !Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
                        jobRequest.cancelAndEdit().build().schedule();
                        restartedInCurrentIteration = true;
                    } else if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.UNMETERED) &&
                            Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
                        jobRequest.cancelAndEdit().build().schedule();
                        restartedInCurrentIteration = true;
                    }
                }

                // Handle case of wifi

                if (!restartedInCurrentIteration) {
                    if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.CONNECTED) &&
                            !Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
                        jobRequest.cancelAndEdit().build().schedule();
                    } else if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.UNMETERED) &&
                            Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
                        jobRequest.cancelAndEdit().build().schedule();
                    }
                }
            } else {
                JobManager.instance().cancel(jobRequest.getJobId());
            }
        }

        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(context.getContentResolver(), context);
        OCUpload[] failedUploads = uploadsStorageManager.getFailedUploads();

        for (OCUpload failedUpload : failedUploads) {
            accountExists = false;
            fileExists = new File(failedUpload.getLocalPath()).exists();
            restartedInCurrentIteration = false;

            // check if accounts still exists
            for (Account account : AccountUtils.getAccounts(context)) {
                if (account.name.equals(failedUpload.getAccountName())) {
                    accountExists = true;
                    break;
                }
            }

            if (!failedUpload.getLastResult().equals(UploadResult.UPLOADED)) {
                if (failedUpload.getCreadtedBy() == UploadFileOperation.CREATED_AS_INSTANT_PICTURE) {
                    if (accountExists && fileExists) {
                        // Handle case of charging

                        if (failedUpload.isWhileChargingOnly() && Device.isCharging(context)) {
                            if (failedUpload.isUseWifiOnly() &&
                                    Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
                                uploadRequester.retry(context, failedUpload);
                                restartedInCurrentIteration = true;
                            } else if (!failedUpload.isUseWifiOnly() &&
                                    !Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
                                uploadRequester.retry(context, failedUpload);
                                restartedInCurrentIteration = true;
                            }
                        }

                        // Handle case of wifi

                        if (!restartedInCurrentIteration) {
                            if (failedUpload.isUseWifiOnly() &&
                                    Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
                                uploadRequester.retry(context, failedUpload);
                            } else if (!failedUpload.isUseWifiOnly() &&
                                    !Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
                                uploadRequester.retry(context, failedUpload);
                            }
                        }
                    }
                } else {
                    uploadsStorageManager.removeUpload(failedUpload);
                }
            } else {
                if (accountExists && fileExists) {
                    uploadRequester.retry(context, failedUpload);
                } else {
                    uploadsStorageManager.removeUpload(failedUpload);
                }
            }
        }
    }
}
}
