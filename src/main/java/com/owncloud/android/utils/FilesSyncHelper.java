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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.jobs.AutoUploadJob;

import java.io.File;

public class FilesSyncHelper {

    public static void insertAllDBEntries() {
        boolean dryRun;

        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + syncedFolder.getId();
                dryRun = TextUtils.isEmpty(arbitraryDataProvider.getValue
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
                    // custom folder, do nothing
                }
            }
        }
    }

    private static void insertContentIntoDB(Uri uri, boolean dryRun, SyncedFolder syncedFolder) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor;
        int column_index_data, column_index_date_modified;

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
        boolean restartedInCurrentIteration = false;

        for (JobRequest jobRequest : JobManager.instance().getAllJobRequestsForTag(AutoUploadJob.TAG)) {
            restartedInCurrentIteration = false;
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
        }
    }
}
