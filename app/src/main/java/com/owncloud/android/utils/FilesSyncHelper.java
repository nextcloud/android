/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Jonas Mayer <jonas.mayer@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.ContentObserverWork;
import com.nextcloud.client.jobs.autoUpload.AutoUploadHelper;
import com.nextcloud.client.jobs.autoUpload.FileSystemRepository;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.utils.extensions.UriExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;

/**
 * Various utilities that make auto upload tick
 */
public final class FilesSyncHelper {
    public static final String TAG = "FileSyncHelper";

    public static final String GLOBAL = "global";

    private FilesSyncHelper() {
        // utility class -> private constructor
    }

    public static void insertAllDBEntriesForSyncedFolder(SyncedFolder syncedFolder, AutoUploadHelper helper, FileSystemRepository repository) {
        final long enabledTimestampMs = syncedFolder.getEnabledTimestampMs();

        if (syncedFolder.isEnabled() && (syncedFolder.isExisting() || enabledTimestampMs >= 0)) {
            MediaFolderType mediaType = syncedFolder.getType();

            if (mediaType == MediaFolderType.IMAGE) {
                repository.insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, syncedFolder);
                repository.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, syncedFolder);
            } else if (mediaType == MediaFolderType.VIDEO) {
                repository.insert(MediaStore.Video.Media.INTERNAL_CONTENT_URI, syncedFolder);
                repository.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, syncedFolder);
            } else {
                helper.insertCustomFolderIntoDB(syncedFolder, repository);
            }
        } else {
            if (!syncedFolder.isEnabled()) {
                Log_OC.w(TAG, "insertAllDBEntriesForSyncedFolder, syncedFolder not enabled");
            }

            if (!syncedFolder.isExisting()) {
                Log_OC.w(TAG, "insertAllDBEntriesForSyncedFolder, syncedFolder is not exists");
            }

            Log_OC.w(TAG, "insertAllDBEntriesForSyncedFolder, enabledTimestampMs: " + enabledTimestampMs);
        }
    }

    /**
     * Attempts to get the file path from a content URI string (e.g., content://media/external/images/media/2281)
     * and checks its type. If the conditions are met, the file is stored for auto-upload.
     * <p>
     * If any attempt fails, the method returns {@code false}.
     *
     * @param syncedFolder The folder marked for auto-upload.
     * @param contentUris  An array of content URI strings collected from {@link ContentObserverWork##checkAndTriggerAutoUpload()}.
     * @return {@code true} if all changed content URIs were successfully stored; {@code false} otherwise.
     */
    public static boolean insertChangedEntries(SyncedFolder syncedFolder, String[] contentUris, FileSystemRepository repository) {
        final Context context = MainApp.getAppContext();
        for (String contentUriString : contentUris) {
            if (contentUriString == null) {
                Log_OC.w(TAG, "null content uri string");
                return false;
            }

            Uri contentUri;
            try {
                contentUri = Uri.parse(contentUriString);
            } catch (Exception e) {
                Log_OC.e(TAG, "Invalid URI: " + contentUriString, e);
                return false;
            }

            String filePath = UriExtensionsKt.toFilePath(contentUri, context);
            if (filePath == null) {
                Log_OC.w(TAG, "File path is null");
                return false;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                Log_OC.w(TAG, "syncedFolder contains not existing changed file: " + filePath);
                return false;
            }

            if (!syncedFolder.containsTypedFile(file, filePath)) {
                Log_OC.w(TAG, "syncedFolder not contains typed file, changedFile: " + filePath);
                return false;
            }

            repository.insertOrReplace(filePath, file.lastModified(), syncedFolder);
        }

        Log_OC.d(TAG, "changed content uris successfully stored");

        return true;
    }

    public static void restartUploadsIfNeeded(final UploadsStorageManager uploadsStorageManager,
                                              final UserAccountManager accountManager,
                                              final ConnectivityService connectivityService,
                                              final PowerManagementService powerManagementService) {
        Log_OC.d(TAG, "restartUploadsIfNeeded, called");
        FileUploadHelper.Companion.instance().retryFailedUploads(
            uploadsStorageManager,
            connectivityService,
            accountManager,
            powerManagementService);
    }

    public static void scheduleFilesSyncForAllFoldersIfNeeded(Context context, SyncedFolderProvider syncedFolderProvider, BackgroundJobManager jobManager) {
        Log_OC.d(TAG, "scheduleFilesSyncForAllFoldersIfNeeded, called");
        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                jobManager.schedulePeriodicFilesSyncJob(syncedFolder);
            }
        }
        if (context != null) {
            jobManager.scheduleContentObserverJob();
        } else {
            Log_OC.w(TAG, "cant scheduleContentObserverJob, context is null");
        }
    }

    public static void startAutoUploadImmediatelyWithContentUris(SyncedFolderProvider syncedFolderProvider, BackgroundJobManager jobManager, boolean overridePowerSaving, String[] contentUris) {
        Log_OC.d(TAG, "startAutoUploadImmediatelyWithContentUris");
        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                jobManager.startAutoUploadImmediately(syncedFolder, overridePowerSaving, contentUris);
            }
        }
    }

    public static void startAutoUploadImmediately(SyncedFolderProvider syncedFolderProvider, BackgroundJobManager jobManager, boolean overridePowerSaving) {
        Log_OC.d(TAG, "startAutoUploadImmediately");
        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                jobManager.startAutoUploadImmediately(syncedFolder, overridePowerSaving, new String[]{});
            }
        }
    }
}
