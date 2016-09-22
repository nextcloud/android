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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * media queries to gain access to media lists for the device.
 */
public class MediaProvider {
    private static final Uri MEDIA_URI = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    /**
     * TODO rewrite
     * Getting All Images Path
     *
     * @param activity
     * @return List with images folders
     */
    public static List<MediaFolder> getAllShownImagesPath(Activity activity) {
        Cursor cursor;
        int column_index_data, column_index_folder_name, column_index_data_image;
        ArrayList<String> listOfAllImages = new ArrayList<>();
        String absolutePathOfImage = null;
        String folderName = null;

        String[] projectionTest = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        //String[] projection = {MediaStore.Images.Media.BUCKET_DISPLAY_NAME,MediaStore.Images.Media.BUCKET_ID};
        String[] folderProjection = new String[]{"Distinct " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore
                .MediaColumns.DATA};
        String[] fileProjection = new String[]{MediaStore.MediaColumns.DATA};
        String folderSelection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " IS NOT NULL) GROUP BY (" + MediaStore
                .Images.Media
                .BUCKET_DISPLAY_NAME;
        String fileSelection = MediaStore.MediaColumns.DATA + " LIKE ";
        String folderSortOrder = "MAX(" + MediaStore.Images.Media.DISPLAY_NAME + ") DESC";
        String fileSortOrder = MediaStore.MediaColumns.DATA + " DESC LIMIT 8"; //  LIMIT 8

        cursor = activity.getContentResolver().query(MEDIA_URI, folderProjection, folderSelection, null, folderSortOrder);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

        List<MediaFolder> mediaFolders = new ArrayList<>();
        while (cursor.moveToNext()) {
            MediaFolder mediaFolder = new MediaFolder();
            absolutePathOfImage = cursor.getString(column_index_data);
            folderName = cursor.getString(column_index_folder_name);
            mediaFolder.path = folderName;
            mediaFolder.folder = absolutePathOfImage.substring(0, absolutePathOfImage.lastIndexOf(folderName) + folderName.length());
            mediaFolder.filePaths = new ArrayList<>();

            Cursor cursorImages = activity.getContentResolver().query(MEDIA_URI, fileProjection, fileSelection + "'" +
                    absolutePathOfImage.substring(0, absolutePathOfImage.lastIndexOf("/")) + "/%'", null,
                    fileSortOrder);
            column_index_data_image = cursorImages.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            Log.e("READ IMAGES", "Reading images for --> " + mediaFolder.folder);
            while (cursorImages.moveToNext()) {
                mediaFolder.filePaths.add(cursorImages.getString(column_index_data_image));
            }

            mediaFolders.add(mediaFolder);
        }

        return mediaFolders;
    }
}
