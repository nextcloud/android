/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
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

package com.owncloud.android.datamodel;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Media queries to gain access to media lists for the device.
 */
public class MediaProvider {
    private static final String TAG = MediaProvider.class.getSimpleName();

    // fixed query parameters
    private static final Uri IMAGES_MEDIA_URI = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final String[] FILE_PROJECTION = new String[]{MediaStore.MediaColumns.DATA};
    private static final String IMAGES_FILE_SELECTION = MediaStore.Images.Media.BUCKET_ID + "=";
    private static final String[] IMAGES_FOLDER_PROJECTION = {"Distinct " + MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
    private static final String IMAGES_FOLDER_SORT_ORDER = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC";

    private static final String[] VIDEOS_FOLDER_PROJECTION = {"Distinct " + MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME};

    /**
     * Getting All Images Paths.
     *
     * @param contentResolver the content resolver
     * @param itemLimit       the number of media items (usually images) to be returned per media folder.
     * @return list with media folders
     */
    public static List<MediaFolder> getImageFolders(ContentResolver contentResolver, int itemLimit,
                                                    @Nullable final Activity activity) {
        // check permissions
        checkPermissions(activity);

        // query media/image folders
        Cursor cursorFolders = null;
        if (activity != null && PermissionUtil.checkSelfPermission(activity.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            cursorFolders = contentResolver.query(IMAGES_MEDIA_URI, IMAGES_FOLDER_PROJECTION, null, null,
                    IMAGES_FOLDER_SORT_ORDER);
        }

        List<MediaFolder> mediaFolders = new ArrayList<>();
        String dataPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder();

        if (cursorFolders != null) {
            String folderName;
            String fileSortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC LIMIT " + itemLimit;
            Cursor cursorImages;

            while (cursorFolders.moveToNext()) {
                String folderId = cursorFolders.getString(cursorFolders.getColumnIndex(MediaStore.Images.Media
                        .BUCKET_ID));

                MediaFolder mediaFolder = new MediaFolder();
                folderName = cursorFolders.getString(cursorFolders.getColumnIndex(
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                mediaFolder.type = MediaFolderType.IMAGE;
                mediaFolder.folderName = folderName;
                mediaFolder.filePaths = new ArrayList<>();

                // query images
                cursorImages = contentResolver.query(
                        IMAGES_MEDIA_URI,
                        FILE_PROJECTION,
                        IMAGES_FILE_SELECTION + folderId,
                        null,
                        fileSortOrder
                );
                Log.d(TAG, "Reading images for " + mediaFolder.folderName);

                if (cursorImages != null) {
                    String filePath;

                    while (cursorImages.moveToNext()) {
                        filePath = cursorImages.getString(cursorImages.getColumnIndexOrThrow(
                                MediaStore.MediaColumns.DATA));

                        // check if valid path
                        if (filePath != null && filePath.lastIndexOf("/") > 0) {
                            mediaFolder.filePaths.add(filePath);
                            mediaFolder.absolutePath = filePath.substring(0, filePath.lastIndexOf("/"));
                        }
                    }
                    cursorImages.close();

                    // only do further work if folder is not within the Nextcloud app itself
                    if (mediaFolder.absolutePath != null && !mediaFolder.absolutePath.startsWith(dataPath)) {

                        // count images
                        Cursor count = contentResolver.query(
                                IMAGES_MEDIA_URI,
                                FILE_PROJECTION,
                                IMAGES_FILE_SELECTION + folderId,
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

    private static void checkPermissions(@Nullable Activity activity) {
        if (activity != null &&
                !PermissionUtil.checkSelfPermission(activity.getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = Snackbar.make(activity.findViewById(R.id.ListLayout),
                        R.string.permission_storage_access, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_ok, v -> PermissionUtil.requestWriteExternalStoreagePermission(activity));

                ThemeUtils.colorSnackbar(activity.getApplicationContext(), snackbar);

                snackbar.show();
            } else {
                // No explanation needed, request the permission.
                PermissionUtil.requestWriteExternalStoreagePermission(activity);
            }
        }
    }

    public static List<MediaFolder> getVideoFolders(ContentResolver contentResolver, int itemLimit,
                                                    @Nullable final Activity activity) {
        // check permissions
        checkPermissions(activity);

        // query media/image folders
        Cursor cursorFolders = null;
        if (activity != null && PermissionUtil.checkSelfPermission(activity.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            cursorFolders = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VIDEOS_FOLDER_PROJECTION,
                    null, null, null);
        } 
        
        List<MediaFolder> mediaFolders = new ArrayList<>();
        String dataPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder();

        if (cursorFolders != null) {
            String folderName;
            String fileSortOrder = MediaStore.Video.Media.DATE_TAKEN + " DESC LIMIT " + itemLimit;
            Cursor cursorVideos;

            while (cursorFolders.moveToNext()) {
                String folderId = cursorFolders.getString(cursorFolders.getColumnIndex(MediaStore.Video.Media
                        .BUCKET_ID));

                MediaFolder mediaFolder = new MediaFolder();
                folderName = cursorFolders.getString(cursorFolders.getColumnIndex(
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
                mediaFolder.type = MediaFolderType.VIDEO;
                mediaFolder.folderName = folderName;
                mediaFolder.filePaths = new ArrayList<>();

                // query videos
                cursorVideos = contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        FILE_PROJECTION,
                        MediaStore.Video.Media.BUCKET_ID + "=" + folderId,
                        null,
                        fileSortOrder);
                Log.d(TAG, "Reading videos for " + mediaFolder.folderName);

                if (cursorVideos != null) {
                    String filePath;
                    while (cursorVideos.moveToNext()) {
                        filePath = cursorVideos.getString(cursorVideos.getColumnIndexOrThrow(
                                MediaStore.MediaColumns.DATA));

                        if (filePath != null) {
                            mediaFolder.filePaths.add(filePath);
                            mediaFolder.absolutePath = filePath.substring(0, filePath.lastIndexOf("/"));
                        }
                    }
                    cursorVideos.close();

                    // only do further work if folder is not within the Nextcloud app itself
                    if (mediaFolder.absolutePath != null && !mediaFolder.absolutePath.startsWith(dataPath)) {

                        // count images
                        Cursor count = contentResolver.query(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                FILE_PROJECTION,
                                MediaStore.Video.Media.BUCKET_ID + "=" + folderId,
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
