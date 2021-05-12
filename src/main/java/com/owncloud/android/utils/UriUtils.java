/**
 * ownCloud Android client application
 *
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Locale;


/**
 * A helper class for some Uri operations.
 */
public final class UriUtils {

    private static final String TAG = UriUtils.class.getSimpleName();

    public static final String URI_CONTENT_SCHEME = "content://";

    private UriUtils() {
        // utility class -> private constructor
    }

    public static String getDisplayNameForUri(Uri uri, Context context) {

        if (uri == null || context == null) {
            throw new IllegalArgumentException("Received NULL!");
        }

        String displayName;

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            displayName = uri.getLastPathSegment();     // ready to return

        } else {
            // content: URI

            displayName = getDisplayNameFromContentResolver(uri, context);

            try {
                if (displayName == null) {
                    // last chance to have a name
                    displayName = uri.getLastPathSegment().replaceAll("\\s", "");
                }

                // Add best possible extension
                int index = displayName.lastIndexOf('.');
                if (index == -1 || MimeTypeMap.getSingleton().
                        getMimeTypeFromExtension(displayName.substring(index + 1).toLowerCase(Locale.ROOT)) == null) {
                    String mimeType = context.getContentResolver().getType(uri);
                    String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) {
                        displayName += "." + extension;
                    }
                }

            } catch (Exception e) {
                Log_OC.e(TAG, "No way to get a display name for " + uri.toString());
            }
        }

        // Replace path separator characters to avoid inconsistent paths
        return displayName.replaceAll("/", "-");
    }


    private static String getDisplayNameFromContentResolver(Uri uri, Context context) {
        String displayName = null;
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            String displayNameColumn;
            if (MimeTypeUtil.isImage(mimeType)) {
                displayNameColumn = MediaStore.Images.ImageColumns.DISPLAY_NAME;

            } else if (MimeTypeUtil.isVideo(mimeType)) {
                displayNameColumn = MediaStore.Video.VideoColumns.DISPLAY_NAME;

            } else if (MimeTypeUtil.isAudio(mimeType)) {
                displayNameColumn = MediaStore.Audio.AudioColumns.DISPLAY_NAME;
            } else {
                displayNameColumn = MediaStore.Files.FileColumns.DISPLAY_NAME;
            }

            try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{displayNameColumn},
                null,
                null,
                null
            )) {
                if (cursor != null) {
                    cursor.moveToFirst();
                    displayName = cursor.getString(cursor.getColumnIndex(displayNameColumn));
                }

            } catch (Exception e) {
                Log_OC.e(TAG, "Could not retrieve display name for " + uri.toString());
                // nothing else, displayName keeps null

            }
        }
        return displayName;
    }


}
