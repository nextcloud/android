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

import java.util.ArrayList;
import java.util.List;

/**
 * Media queries to gain access to media lists for the device.
 */
public class MediaProvider {
    private static final String TAG = MediaProvider.class.getSimpleName();
    private static final Uri MEDIA_URI = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    /**
     * Getting All Images Paths.
     *
     * @param contentResolver the content resolver
     * @param itemLimit       the number of media items (usually images) to be returned per media folder.
     * @return list with media folders
     */
    public static List<MediaFolder> getMediaFolders(ContentResolver contentResolver, int itemLimit) {
        Cursor cursor;
        List<MediaFolder> mediaFolders = new ArrayList<>();
        int column_index_data, column_index_folder_name, column_index_data_image;
        String absolutePathOfImage;
        String folderName;

        // TODO rewrite/beautify

        String[] folderProjection = new String[]{"Distinct " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore
                .MediaColumns.DATA, MediaStore.Images.Media.DATE_TAKEN};
        String[] fileProjection = new String[]{MediaStore.MediaColumns.DATA};
        String folderSelection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " IS NOT NULL) GROUP BY (" + MediaStore
                .Images.Media
                .BUCKET_DISPLAY_NAME;
        String fileSelection = MediaStore.MediaColumns.DATA + " LIKE ";
        String folderSortOrder = "MAX(" + MediaStore.MediaColumns.DATA + ") DESC";
        String fileSortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC LIMIT "+ itemLimit;

        cursor = contentResolver.query(MEDIA_URI, folderProjection, folderSelection, null, folderSortOrder);

        if (cursor == null) {
            return mediaFolders;
        }

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        Cursor cursorImages;

        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            cursorImages = contentResolver.query(MEDIA_URI, fileProjection, fileSelection + "'" +
                            absolutePathOfImage.substring(0, absolutePathOfImage.lastIndexOf("/")) + "/%'", null,
                    fileSortOrder);

            if (cursorImages != null) {

                MediaFolder mediaFolder = new MediaFolder();
                folderName = cursor.getString(column_index_folder_name);
                mediaFolder.folderName = folderName;
                mediaFolder.absolutePath = absolutePathOfImage.substring(0, absolutePathOfImage.lastIndexOf(folderName) +
                        folderName
                                .length());
                mediaFolder.filePaths = new ArrayList<>();

                column_index_data_image = cursorImages.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                Log.d(TAG, "Reading images for " + mediaFolder.absolutePath);
                while (cursorImages.moveToNext()) {
                    mediaFolder.filePaths.add(cursorImages.getString(column_index_data_image));
                }
                cursorImages.close();

                mediaFolder.numberOfFiles = contentResolver.query(
                        MEDIA_URI,
                        fileProjection,
                        fileSelection + "'"
                                + absolutePathOfImage.substring(0, absolutePathOfImage.lastIndexOf("/")) + "/%'",
                        null,
                        null).getCount();

                mediaFolders.add(mediaFolder);
            }
        }
        cursor.close();

        return mediaFolders;
    }
}
