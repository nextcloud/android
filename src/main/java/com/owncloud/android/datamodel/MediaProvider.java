/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.owncloud.android.MainApp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Media queries to gain access to media lists for the device.
 */
public class MediaProvider {
    private static final String TAG = MediaProvider.class.getSimpleName();

    // fixed query parameters
    private static final Uri MEDIA_URI = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final String[] FILE_PROJECTION = new String[]{MediaStore.MediaColumns.DATA};
    private static final String FILE_SELECTION = MediaStore.Images.Media.BUCKET_ID + "=";
    private static final String[] FOLDER_PROJECTION = { "Distinct " + MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME };
    private static final String FOLDER_SORT_ORDER = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC";

    /**
     * Getting All Images Paths.
     *
     * @param contentResolver the content resolver
     * @param itemLimit       the number of media items (usually images) to be returned per media folder.
     * @return list with media folders
     */
    public static List<MediaFolder> getMediaFolders(ContentResolver contentResolver, int itemLimit) {
        // query media/image folders
        Cursor cursorFolders = contentResolver.query(MEDIA_URI, FOLDER_PROJECTION, null, null, FOLDER_SORT_ORDER);
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
                mediaFolder.folderName = folderName;
                mediaFolder.filePaths = new ArrayList<>();

                // query images
                cursorImages = contentResolver.query(MEDIA_URI, FILE_PROJECTION, FILE_SELECTION + folderId, null,
                        fileSortOrder);
                Log.d(TAG, "Reading images for " + mediaFolder.folderName);

                if (cursorImages != null) {
                    String filePath;
                    while (cursorImages.moveToNext()) {
                        filePath = cursorImages.getString(cursorImages.getColumnIndexOrThrow(
                                MediaStore.MediaColumns.DATA));
                        mediaFolder.filePaths.add(filePath);
                        mediaFolder.absolutePath = filePath.substring(0, filePath.lastIndexOf("/"));
                    }
                    cursorImages.close();

                    // only do further work if folder is not within the Nextcloud app itself
                    if (!mediaFolder.absolutePath.startsWith(dataPath)) {

                        // count images
                        Cursor count = contentResolver.query(
                                MEDIA_URI,
                                FILE_PROJECTION,
                                FILE_SELECTION + folderId,
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
