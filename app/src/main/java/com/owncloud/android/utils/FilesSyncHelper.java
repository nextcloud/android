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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.ContentObserverWork;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.utils.extensions.UriExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.lukhnos.nnio.file.AccessDeniedException;
import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.FileVisitor;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;
import org.lukhnos.nnio.file.impl.FileBasedPathImpl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

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

    /**
     * Copy of {@link Files#walkFileTree(Path, FileVisitor)} that walks the file tree in random order.
     *
     * @see org.lukhnos.nnio.file.Files#walkFileTree(Path, FileVisitor)
     */
    public static void walkFileTreeRandomly(Path start, FileVisitor<? super Path> visitor) throws IOException {
        File file = start.toFile();
        if (!file.canRead()) {
            Log_OC.d(TAG, "walkFileTreeRandomly, cant read the file: " + file.getAbsolutePath());
            visitor.visitFileFailed(start, new AccessDeniedException(file.toString()));
        } else {
            Log_OC.d(TAG, "walkFileTreeRandomly, reading file: " + file.getAbsolutePath());

            if (Files.isDirectory(start)) {
                Log_OC.d(TAG, "walkFileTreeRandomly, file is directory: " + file.getAbsolutePath());

                FileVisitResult preVisitDirectoryResult = visitor.preVisitDirectory(start, null);
                if (preVisitDirectoryResult == FileVisitResult.CONTINUE) {
                    Log_OC.d(TAG, "walkFileTreeRandomly, preVisitDirectoryResult == FileVisitResult.CONTINUE");
                    File[] children = start.toFile().listFiles();
                    if (children != null) {
                        Log_OC.d(TAG, "walkFileTreeRandomly, children exists");

                        Collections.shuffle(Arrays.asList(children));
                        File[] var5 = children;
                        int var6 = children.length;

                        for(int var7 = 0; var7 < var6; ++var7) {
                            Log_OC.d(TAG, "walkFileTreeRandomly -- recursive call");
                            File child = var5[var7];
                            walkFileTreeRandomly(FileBasedPathImpl.get(child), visitor);
                        }

                        visitor.postVisitDirectory(start, null);
                    } else {
                        Log_OC.w(TAG, "walkFileTreeRandomly, children is null");
                    }
                } else {
                    Log_OC.w(TAG, "walkFileTreeRandomly, preVisitDirectoryResult != FileVisitResult.CONTINUE");
                }
            } else {
                Log_OC.d(TAG, "walkFileTreeRandomly, file is not directory");
                visitor.visitFile(start, new BasicFileAttributes(file));
            }
        }
    }

    private static void insertCustomFolderIntoDB(Path path,
                                                 SyncedFolder syncedFolder,
                                                 FilesystemDataProvider filesystemDataProvider,
                                                 long lastCheck) {
        Log_OC.d(TAG, "insertCustomFolderIntoDB called");
        final long enabledTimestampMs = syncedFolder.getEnabledTimestampMs();

        try {
            walkFileTreeRandomly(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    File file = path.toFile();
                    if (syncedFolder.isExcludeHidden() && file.isHidden()) {
                        Log_OC.w(TAG, "skipping files, exclude hidden file/folder: " + path);
                        // exclude hidden file or folder
                        return FileVisitResult.CONTINUE;
                    }

                    if (attrs.lastModifiedTime().toMillis() < lastCheck) {
                        Log_OC.w(TAG, "skipping files that already checked: " + path);
                        // skip files that were already checked
                        return FileVisitResult.CONTINUE;
                    }

                    if (syncedFolder.isExisting() || attrs.lastModifiedTime().toMillis() >= enabledTimestampMs) {
                        // storeOrUpdateFileValue takes a few ms
                        // -> Rest of this file check takes not even 1 ms.
                        filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                                                      attrs.lastModifiedTime().toMillis(),
                                                                      file.isDirectory(), syncedFolder);
                    } else {
                        Log_OC.w(TAG, "skipping files. SynchedFolder not exists or enabledTimestampMs not meeting condition" + path);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (syncedFolder.isExcludeHidden() && dir.compareTo(Paths.get(syncedFolder.getLocalPath())) != 0 && dir.toFile().isHidden()) {
                        Log_OC.d(TAG, "skipping hidden path: " + dir.getFileName());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Log_OC.e(TAG, "Something went wrong while indexing files for auto upload: " + e.getLocalizedMessage());
        }
    }

    public static void insertAllDBEntriesForSyncedFolder(SyncedFolder syncedFolder) {
        Log_OC.d(TAG, "insertAllDBEntriesForSyncedFolder, called. ID: " + syncedFolder.getId());

        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        final long enabledTimestampMs = syncedFolder.getEnabledTimestampMs();

        if (syncedFolder.isEnabled() && (syncedFolder.isExisting() || enabledTimestampMs >= 0)) {
            MediaFolderType mediaType = syncedFolder.getType();
            final long lastCheckTimestampMs = syncedFolder.getLastScanTimestampMs();

            Log_OC.d(TAG,"File-sync start check folder "+syncedFolder.getLocalPath());
            long startTime = System.nanoTime();

            if (mediaType == MediaFolderType.IMAGE) {
                Log_OC.d(TAG, "inserting IMAGE");
                FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                                                    syncedFolder,
                                                    lastCheckTimestampMs);
                FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                    syncedFolder,
                                                    lastCheckTimestampMs);
            } else if (mediaType == MediaFolderType.VIDEO) {
                Log_OC.d(TAG, "inserting VIDEO");
                FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                                                    syncedFolder,
                                                    lastCheckTimestampMs);
                FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                    syncedFolder,
                                                    lastCheckTimestampMs);
            } else {
                Log_OC.d(TAG, "inserting other media types: " + mediaType.toString());
                FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
                Path path = Paths.get(syncedFolder.getLocalPath());
                FilesSyncHelper.insertCustomFolderIntoDB(path, syncedFolder, filesystemDataProvider, lastCheckTimestampMs);
            }

            Log_OC.d(TAG,"File-sync finished full check for custom folder "+syncedFolder.getLocalPath()+" within "+(System.nanoTime() - startTime)+ "ns");
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
    public static boolean insertChangedEntries(SyncedFolder syncedFolder, String[] contentUris) {
        Log_OC.d(TAG, "insertChangedEntries, syncedFolderID: " + syncedFolder.getId());
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
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

            filesystemDataProvider.storeOrUpdateFileValue(filePath, file.lastModified(), file.isDirectory(), syncedFolder);
        }

        Log_OC.d(TAG, "changed content uris successfully stored");

        return true;
    }

    private static void insertContentIntoDB(Uri uri, SyncedFolder syncedFolder,
                                            long lastCheckTimestampMs) {
        Log_OC.d(TAG, "insertContentIntoDB, URI: " + uri + " syncedFolderID: " + syncedFolder.getId() + " lastCheckTimestampMs " + lastCheckTimestampMs);
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
            Log_OC.w(TAG, "path is not ending with: " + PATH_SEPARATOR);
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

                if (syncedFolder.getLastScanTimestampMs() != SyncedFolder.NOT_SCANNED_YET &&
                    cursor.getLong(column_index_date_modified) < (lastCheckTimestampMs / 1000)) {
                    Log_OC.w(TAG, "skipping contentPath");
                    continue;
                }

                if (syncedFolder.isExisting() || cursor.getLong(column_index_date_modified) >= enabledTimestampMs / 1000) {
                    // storeOrUpdateFileValue takes a few ms
                    // -> Rest of this file check takes not even 1 ms.
                    filesystemDataProvider.storeOrUpdateFileValue(contentPath,
                                                                  cursor.getLong(column_index_date_modified), isFolder,
                                                                  syncedFolder);
                } else {
                    if (!syncedFolder.isExisting()) {
                        Log_OC.w(TAG, "syncedFolder not exists");
                    }

                    if (cursor.getLong(column_index_date_modified) < enabledTimestampMs / 1000) {
                        Log_OC.w(TAG, "column_index_date_modified not meeting condition");
                    }
                }
            }
            cursor.close();
        } else {
            Log_OC.w(TAG, "cursor is null ");
        }
    }

    public static void restartUploadsIfNeeded(final UploadsStorageManager uploadsStorageManager,
                                              final UserAccountManager accountManager,
                                              final ConnectivityService connectivityService,
                                              final PowerManagementService powerManagementService) {
        Log_OC.d(TAG, "restartUploadsIfNeeded, called");
        new Thread(() -> FileUploadHelper.Companion.instance().retryFailedUploads(
            uploadsStorageManager,
            connectivityService,
            accountManager,
            powerManagementService)).start();
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
