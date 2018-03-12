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

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.jobs.FilesSyncJob;
import com.owncloud.android.jobs.NContentObserverJob;
import com.owncloud.android.jobs.OfflineSyncJob;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/*
    Various utilities that make auto upload tick
 */
public class FilesSyncHelper {
    public static final String TAG = "FileSyncHelper";

    public static final String GLOBAL = "global";
    public static final String SYNCEDFOLDERINITIATED = "syncedFolderIntitiated_";

    public static int ContentSyncJobId = 315;

    public static void insertAllDBEntriesForSyncedFolder(SyncedFolder syncedFolder) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);

        Long currentTime = System.currentTimeMillis();
        double currentTimeInSeconds = currentTime / 1000.0;
        String currentTimeString = Long.toString((long) currentTimeInSeconds);

        String syncedFolderInitiatedKey = SYNCEDFOLDERINITIATED + syncedFolder.getId();
        boolean dryRun = TextUtils.isEmpty(arbitraryDataProvider.getValue
                (GLOBAL, syncedFolderInitiatedKey));

        if (MediaFolderType.IMAGE == syncedFolder.getType()) {
            if (dryRun) {
                arbitraryDataProvider.storeOrUpdateKeyValue(GLOBAL, syncedFolderInitiatedKey,
                        currentTimeString);
            } else {
                FilesSyncHelper.insertContentIntoDB(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI
                        , syncedFolder);
                FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        syncedFolder);
            }

        } else if (MediaFolderType.VIDEO == syncedFolder.getType()) {

            if (dryRun) {
                arbitraryDataProvider.storeOrUpdateKeyValue(GLOBAL, syncedFolderInitiatedKey,
                        currentTimeString);
            } else {
                FilesSyncHelper.insertContentIntoDB(android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                        syncedFolder);
                FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        syncedFolder);
            }

        } else {
            try {

                if (dryRun) {
                    arbitraryDataProvider.storeOrUpdateKeyValue(GLOBAL, syncedFolderInitiatedKey,
                            currentTimeString);
                } else {
                    FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
                    Path path = Paths.get(syncedFolder.getLocalPath());

                    String dateInitiated = arbitraryDataProvider.getValue(GLOBAL,
                            syncedFolderInitiatedKey);

                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                            File file = path.toFile();
                            if (attrs.lastModifiedTime().toMillis() >= Long.parseLong(dateInitiated) * 1000) {
                                filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                        attrs.lastModifiedTime().toMillis(), file.isDirectory(), syncedFolder);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });

                }

            } catch (IOException e) {
                Log.e(TAG, "Something went wrong while indexing files for auto upload " + e.getLocalizedMessage());
            }
        }
    }

    public static void insertAllDBEntries(boolean skipCustom) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if ((syncedFolder.isEnabled()) && ((MediaFolderType.CUSTOM != syncedFolder.getType()) || !skipCustom)) {
                insertAllDBEntriesForSyncedFolder(syncedFolder);
            }
        }
    }

    private static void insertContentIntoDB(Uri uri, SyncedFolder syncedFolder) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);

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

        String syncedFolderInitiatedKey = SYNCEDFOLDERINITIATED + syncedFolder.getId();
        String dateInitiated = arbitraryDataProvider.getValue(GLOBAL, syncedFolderInitiatedKey);

        cursor = context.getContentResolver().query(uri, projection, MediaStore.MediaColumns.DATA + " LIKE ?",
                new String[]{path}, null);

        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_date_modified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            while (cursor.moveToNext()) {
                contentPath = cursor.getString(column_index_data);
                isFolder = new File(contentPath).isDirectory();
                if (cursor.getLong(column_index_date_modified) >= Long.parseLong(dateInitiated)) {
                    filesystemDataProvider.storeOrUpdateFileValue(contentPath,
                            cursor.getLong(column_index_date_modified), isFolder, syncedFolder);
                }
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

        new Thread(() -> {
            if (!Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY) &&
                    !ConnectivityUtils.isInternetWalled(context)) {
                uploadRequester.retryFailedUploads(context, null, null);
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
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

    public static void scheduleNJobs(boolean force, Context context) {
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(context.getContentResolver());


        boolean hasVideoFolders = false;
        boolean hasImageFolders = false;

        if (syncedFolderProvider.getSyncedFolders() != null) {
            for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
                if (MediaFolderType.VIDEO == syncedFolder.getType()) {
                    hasVideoFolders = true;
                } else if (MediaFolderType.IMAGE == syncedFolder.getType()) {
                    hasImageFolders = true;
                }
            }
        }

        if (hasImageFolders || hasVideoFolders) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                scheduleJobOnN(hasImageFolders, hasVideoFolders, force);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cancelJobOnN();
            }
        }
    }

    public static void scheduleFilesSyncIfNeeded(Context context) {
        // always run this because it also allows us to perform retries of manual uploads
        new JobRequest.Builder(FilesSyncJob.TAG)
                .setPeriodic(900000L, 300000L)
                .setUpdateCurrent(true)
                .build()
                .schedule();

        if (context != null) {
            scheduleNJobs(false, context);
        }
    }

    public static void scheduleOfflineSyncIfNeeded() {
        Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequestsForTag(OfflineSyncJob.TAG);
        if (jobRequests.size() == 0) {
            new JobRequest.Builder(OfflineSyncJob.TAG)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                    .setUpdateCurrent(false)
                    .build()
                    .schedule();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void cancelJobOnN() {
        JobScheduler jobScheduler = MainApp.getAppContext().getSystemService(JobScheduler.class);
        if (isContentObserverJobScheduled()) {
            jobScheduler.cancel(ContentSyncJobId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void scheduleJobOnN(boolean hasImageFolders, boolean hasVideoFolders,
                                       boolean force) {
        JobScheduler jobScheduler = MainApp.getAppContext().getSystemService(JobScheduler.class);

        if ((hasImageFolders || hasVideoFolders) && (!isContentObserverJobScheduled() || force)) {
            JobInfo.Builder builder = new JobInfo.Builder(ContentSyncJobId, new ComponentName(MainApp.getAppContext(),
                    NContentObserverJob.class.getName()));
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
            builder.setTriggerContentMaxDelay(1500);
            jobScheduler.schedule(builder.build());
        }
    }
}

