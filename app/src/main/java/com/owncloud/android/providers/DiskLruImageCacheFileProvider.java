/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.android.AndroidInjection;

public class DiskLruImageCacheFileProvider extends ContentProvider {
    public static final String TAG = DiskLruImageCacheFileProvider.class.getSimpleName();

    @Inject
    protected UserAccountManager accountManager;

    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);
        return true;
    }

    private OCFile getFile(Uri uri) {
        User user = accountManager.getUser();
        FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(user.toPlatformAccount(),
                MainApp.getAppContext().getContentResolver());

        return fileDataStorageManager.getFileByPath(uri.getPath());
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        return getParcelFileDescriptorForOCFile(getFile(uri));
    }

    public static ParcelFileDescriptor getParcelFileDescriptorForOCFile(OCFile ocFile) throws FileNotFoundException {
        Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + ocFile.getRemoteId());

        // fallback to thumbnail
        if (thumbnail == null) {
            thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                ThumbnailsCacheManager.PREFIX_THUMBNAIL + ocFile.getRemoteId());
        }

        // fallback to default image
        if (thumbnail == null) {
            thumbnail = ThumbnailsCacheManager.mDefaultImg;
        }

        // create a file to write bitmap data
        File f = new File(MainApp.getAppContext().getCacheDir(), ocFile.getFileName());
        try {
            f.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.PNG, 90, bos);
            byte[] bitmapData = bos.toByteArray();

            //write the bytes in file
            try (FileOutputStream fos = new FileOutputStream(f)){
                fos.write(bitmapData);
            } catch (FileNotFoundException e) {
                Log_OC.e(TAG, "File not found: " + e.getMessage());
            }

        } catch (Exception e) {
            Log_OC.e(TAG, "Error opening file: " + e.getMessage());
        }

        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        OCFile ocFile = getFile(uri);
        return ocFile.getMimeType();
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] arg1, String arg2, String[] arg3, String arg4) {
        MatrixCursor cursor = null;

        OCFile ocFile = getFile(uri);
        File file = new File(MainApp.getAppContext().getCacheDir(), ocFile.getFileName());
        if (file.exists()) {
            cursor = new MatrixCursor(new String[] {
                    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE });
            cursor.addRow(new Object[] { uri.getLastPathSegment(),
                    file.length() });
        }

        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
