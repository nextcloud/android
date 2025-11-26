/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Media queries to gain access to media lists for the device.
 */
public final class MediaProvider {
    private static final String TAG = MediaProvider.class.getSimpleName();

    // fixed query parameters
    private static final Uri IMAGES_MEDIA_URI = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final String[] FILE_PROJECTION = new String[]{MediaStore.MediaColumns.DATA};
    private static final String IMAGES_FILE_SELECTION = MediaStore.Images.Media.BUCKET_ID + "=";
    private static final String[] IMAGES_FOLDER_PROJECTION = {MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
    private static final String IMAGES_FOLDER_SORT_COLUMN = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
    private static final String IMAGES_SORT_DIRECTION = ContentResolverHelper.SORT_DIRECTION_ASCENDING;

    private static final String[] VIDEOS_FOLDER_PROJECTION = {MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME};

    private MediaProvider() {
        // utility class -> private constructor
    }

    /**
     * Getting All Images Paths.
     *
     * @param contentResolver the content resolver
     * @param itemLimit       the number of media items (usually images) to be returned per media folder.
     * @return list with media folders
     */
    public static List<MediaFolder> getImageFolders(ContentResolver contentResolver,
                                                    int itemLimit,
                                                    @Nullable final AppCompatActivity activity,
                                                    boolean getWithoutActivity) {
        // check permissions
        checkPermissions(activity);

        // query media/image folders
        Cursor cursorFolders = null;
        if (activity != null && PermissionUtil.checkStoragePermission(activity.getApplicationContext())
            || getWithoutActivity) {
            cursorFolders = ContentResolverHelper.queryResolver(contentResolver, IMAGES_MEDIA_URI,
                                                                IMAGES_FOLDER_PROJECTION, null, null,
                                                                IMAGES_FOLDER_SORT_COLUMN, IMAGES_SORT_DIRECTION, null);
        }

        List<MediaFolder> mediaFolders = new ArrayList<>();
        String dataPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder();

        if (cursorFolders != null) {
            Cursor cursorImages;

            Map<String, String> uniqueFolders = new HashMap<>();

            // since sdk 29 we have to manually distinct on bucket id
            while (cursorFolders.moveToNext()) {
                uniqueFolders.put(cursorFolders.getString(
                    cursorFolders.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)),
                                  cursorFolders.getString(
                                      cursorFolders.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                );
            }
            cursorFolders.close();

            for (Map.Entry<String, String> folder : uniqueFolders.entrySet()) {
                MediaFolder mediaFolder = new MediaFolder();

                mediaFolder.type = MediaFolderType.IMAGE;
                mediaFolder.folderName = folder.getValue();
                mediaFolder.filePaths = new ArrayList<>();

                // query images
                cursorImages = ContentResolverHelper.queryResolver(contentResolver,
                                                                   IMAGES_MEDIA_URI,
                                                                   FILE_PROJECTION,
                                                                   IMAGES_FILE_SELECTION + folder.getKey(),
                                                                   null,
                                                                   MediaStore.Images.Media.DATE_TAKEN,
                                                                   ContentResolverHelper.SORT_DIRECTION_DESCENDING,
                                                                   itemLimit);
                Log_OC.d(TAG, "Reading images for " + mediaFolder.folderName);

                if (cursorImages != null) {
                    String filePath;
                    int imageCount = 0;
                    while (cursorImages.moveToNext() && imageCount < itemLimit) {
                        filePath = cursorImages.getString(cursorImages.getColumnIndexOrThrow(
                            MediaStore.MediaColumns.DATA));

                        // check if valid path and file exists
                        if (isValidAndExistingFilePath(filePath)) {
                            mediaFolder.filePaths.add(filePath);
                            mediaFolder.absolutePath = filePath.substring(0, filePath.lastIndexOf('/'));
                        }
                        // ensure we don't go over the limit due to faulty android implementations
                        imageCount++;
                    }
                    cursorImages.close();

                    // only do further work if folder is not within the Nextcloud app itself
                    if (isFolderOutsideOfAppPath(dataPath, mediaFolder)) {

                        // count images
                        Cursor count = contentResolver.query(
                            IMAGES_MEDIA_URI,
                            FILE_PROJECTION,
                            IMAGES_FILE_SELECTION + folder.getKey(),
                            null,
                            null);

                        if (count != null) {
                            mediaFolder.numberOfFiles = count.getCount();
                            count.close();
                        }

                        mediaFolders.add(mediaFolder);
                    }
                }
            }
        }

        return mediaFolders;
    }

    private static boolean isFolderOutsideOfAppPath(String dataPath, MediaFolder mediaFolder) {
        return mediaFolder.absolutePath != null && !mediaFolder.absolutePath.startsWith(dataPath);
    }

    private static boolean isValidAndExistingFilePath(String filePath) {
        return filePath != null && filePath.lastIndexOf('/') > 0 && new File(filePath).exists();
    }

    private static void checkPermissions(@Nullable AppCompatActivity activity) {
        if (activity != null &&
            !PermissionUtil.checkStoragePermission(activity.getApplicationContext())) {
            PermissionUtil.requestStoragePermissionIfNeeded(activity);
        }
    }

    public static List<MediaFolder> getVideoFolders(ContentResolver contentResolver,
                                                    int itemLimit,
                                                    @Nullable final AppCompatActivity activity,
                                                    boolean getWithoutActivity) {
        // check permissions
        checkPermissions(activity);

        // query media/image folders
        Cursor cursorFolders = null;
        if ((activity != null && PermissionUtil.checkStoragePermission(activity.getApplicationContext()))
            || getWithoutActivity) {
            cursorFolders = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VIDEOS_FOLDER_PROJECTION,
                                                  null, null, null);
        }

        List<MediaFolder> mediaFolders = new ArrayList<>();
        String dataPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder();

        if (cursorFolders != null) {
            Cursor cursorVideos;

            Map<String, String> uniqueFolders = new HashMap<>();

            // since sdk 29 we have to manually distinct on bucket id
            while (cursorFolders.moveToNext()) {
                uniqueFolders.put(cursorFolders.getString(
                    cursorFolders.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)),
                                  cursorFolders.getString(
                                      cursorFolders.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                );
            }
            cursorFolders.close();

            for (Map.Entry<String, String> folder : uniqueFolders.entrySet()) {
                MediaFolder mediaFolder = new MediaFolder();
                mediaFolder.type = MediaFolderType.VIDEO;
                mediaFolder.folderName = folder.getValue();
                mediaFolder.filePaths = new ArrayList<>();

                // query videos
                cursorVideos = ContentResolverHelper.queryResolver(contentResolver,
                                                                   MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                                   FILE_PROJECTION,
                                                                   MediaStore.Video.Media.BUCKET_ID + "=" + folder.getKey(),
                                                                   null,
                                                                   MediaStore.Video.Media.DATE_TAKEN,
                                                                   ContentResolverHelper.SORT_DIRECTION_DESCENDING,
                                                                   itemLimit);

                Log_OC.d(TAG, "Reading videos for " + mediaFolder.folderName);

                if (cursorVideos != null) {
                    String filePath;
                    int videoCount = 0;
                    while (cursorVideos.moveToNext() && videoCount < itemLimit) {
                        filePath = cursorVideos.getString(cursorVideos.getColumnIndexOrThrow(
                            MediaStore.MediaColumns.DATA));

                        if (filePath != null) {
                            mediaFolder.filePaths.add(filePath);
                            mediaFolder.absolutePath = filePath.substring(0, filePath.lastIndexOf('/'));
                        }
                        // ensure we don't go over the limit due to faulty android implementations
                        videoCount++;
                    }
                    cursorVideos.close();

                    // only do further work if folder is not within the Nextcloud app itself
                    if (isFolderOutsideOfAppPath(dataPath, mediaFolder)) {

                        // count images
                        Cursor count = contentResolver.query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            FILE_PROJECTION,
                            MediaStore.Video.Media.BUCKET_ID + "=" + folder.getKey(),
                            null,
                            null);

                        if (count != null) {
                            mediaFolder.numberOfFiles = count.getCount();
                            count.close();
                        }

                        mediaFolders.add(mediaFolder);
                    }
                }
            }
            cursorFolders.close();
        }

        return mediaFolders;
    }
}
