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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.owncloud.android.lib.common.utils.Log_OC;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * A helper class for some Uri operations.
 */
public class UriUtils {

    public static final String TAG = UriUtils.class.getSimpleName();

    public static final String URI_CONTENT_SCHEME = "content://";


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     *
     * @param uri The Uri to check.
     * @return Whether the Uri is from a content provider as kind "content://..."
     */
    public static boolean isContentDocument(Uri uri) {
        return uri.toString().startsWith(URI_CONTENT_SCHEME);
    }


    /**
     * Translates a content:// URI referred to a local file file to a path on the local filesystem
     *
     * @param uri       The URI to resolve
     * @return The path in the file system to the content or null if it could not be found (not a file)
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @SuppressFBWarnings("Bx")
    public static String getLocalPath(Uri uri, Context context) {
        final boolean isKitKatOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKatOrLater && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (UriUtils.isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (UriUtils.isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return UriUtils.getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (UriUtils.isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return UriUtils.getDataColumn(context, contentUri, selection, selectionArgs);
            }
            // Documents providers returned as content://...
            else if (UriUtils.isContentDocument(uri)) {
                return uri.toString();
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (UriUtils.isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            return UriUtils.getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    public static String getDisplayNameForUri(Uri uri, Context context) {

        if (uri == null || context == null) {
            throw new IllegalArgumentException("Received NULL!");
        }

        String displayName = null;

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
                        getMimeTypeFromExtension(displayName.substring(index + 1)) == null) {
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

            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(
                        uri,
                        new String[]{displayNameColumn},
                        null,
                        null,
                        null
                );
                if (cursor != null) {
                    cursor.moveToFirst();
                    displayName = cursor.getString(cursor.getColumnIndex(displayNameColumn));
                }

            } catch (Exception e) {
                Log_OC.e(TAG, "Could not retrieve display name for " + uri.toString());
                // nothing else, displayName keeps null

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return displayName;
    }


}
