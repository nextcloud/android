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
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.evernote.android.job.JobRequest;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.jobs.FilesSyncJob;
import com.owncloud.android.jobs.M1ContentObserverJob;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FilesSyncHelper {
    public static final String TAG = "FileSyncHelper";

    public static int ContentSyncJobId = 315;

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
                        filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                attrs.lastModifiedTime().toMillis(), file.isDirectory(), syncedFolder, dryRun);


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

    public static void insertAllDBEntries(boolean skipCustom) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                if (!skipCustom || MediaFolder.CUSTOM != syncedFolder.getType())
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
                filesystemDataProvider.storeOrUpdateFileValue(contentPath,
                        cursor.getLong(column_index_date_modified), isFolder, syncedFolder, dryRun);
            }
            cursor.close();
        }
    }

    public static void restartJobsIfNeeded() {
        final Context context = MainApp.getAppContext();

        FileUploader.UploadRequester uploadRequester = new FileUploader.UploadRequester();

        boolean accountExists;

        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(context.getContentResolver(), context);
        OCUpload[] failedUploads = uploadsStorageManager.getFailedUploads();

        for (OCUpload failedUpload : failedUploads) {
            accountExists = false;

            // check if accounts still exists
            for (Account account : AccountUtils.getAccounts(context)) {
                if (account.name.equals(failedUpload.getAccountName())) {
                    accountExists = true;
                    break;
                }
            }

            if (!accountExists) {
                uploadsStorageManager.removeUpload(failedUpload);
            }
        }

        uploadRequester.retryFailedUploads(
                context,
                null,
                null
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isContentObserverJobScheduled() {
        JobScheduler js = MainApp.getAppContext().getSystemService(JobScheduler.class);
        List<JobInfo> jobs = js.getAllPendingJobs();

        if (jobs == null || jobs.size() == 0) {
            return false;
        }

        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId() == ContentSyncJobId) {
                return true;
            }
        }

        return false;
    }

    public static void scheduleM1Jobs() {
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(MainApp.getAppContext().
                getContentResolver());


        boolean hasCustomFolders = false;
        boolean hasVideoFolders = false;
        boolean hasImageFolders = false;

        if (syncedFolderProvider.getSyncedFolders() != null) {
            for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
                if (MediaFolder.CUSTOM == syncedFolder.getType()) {
                    hasCustomFolders = true;
                } else if (MediaFolder.VIDEO == syncedFolder.getType()) {
                    hasVideoFolders = true;
                } else if (MediaFolder.IMAGE == syncedFolder.getType()) {
                    hasImageFolders = true;
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            if (hasImageFolders || hasVideoFolders) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    scheduleJobOnM1(hasImageFolders, hasVideoFolders);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cancelJobOnM1();
                }
            }
        }
    }

    public static void scheduleFilesSyncIfNeeded() {
        // always run this because it also allows us to perform retries of manual uploads
        new JobRequest.Builder(FilesSyncJob.TAG)
                .setPeriodic(900000L, 300000L)
                .setUpdateCurrent(true)
                .build()
                .schedule();

        scheduleM1Jobs();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void cancelJobOnM1() {
        JobScheduler jobScheduler = MainApp.getAppContext().getSystemService(JobScheduler.class);
        if (isContentObserverJobScheduled()) {
            jobScheduler.cancel(ContentSyncJobId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void scheduleJobOnM1(boolean hasImageFolders, boolean hasVideoFolders) {
        JobScheduler jobScheduler = MainApp.getAppContext().getSystemService(JobScheduler.class);

        if ((android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) && (hasImageFolders || hasVideoFolders)) {
            if (!isContentObserverJobScheduled()) {
                JobInfo.Builder builder = new JobInfo.Builder(ContentSyncJobId, new ComponentName(MainApp.getAppContext(),
                        M1ContentObserverJob.class.getName()));
                builder.addTriggerContentUri(new JobInfo.TriggerContentUri(android.provider.MediaStore.
                        Images.Media.INTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
                builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.
                        Images.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
                builder.addTriggerContentUri(new JobInfo.TriggerContentUri(android.provider.MediaStore.
                        Video.Media.INTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
                builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.
                        Video.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
                builder.setPersisted(true);
                builder.setTriggerContentMaxDelay(500);
                jobScheduler.schedule(builder.build());
            }
        }

    }
}

