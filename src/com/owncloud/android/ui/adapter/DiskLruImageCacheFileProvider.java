/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 Tobias Kaminsky
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class DiskLruImageCacheFileProvider extends ContentProvider {
    private static String TAG = FileDataStorageManager.class.getSimpleName();
    private FileDataStorageManager mFileDataStorageManager;

    public static final String AUTHORITY = "org.nextcloud.beta.imageCache.provider";

    @Override
    public boolean onCreate() {
        return true;
    }

    private OCFile getFile(Uri uri){
        Account account = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
        mFileDataStorageManager = new FileDataStorageManager(account,
                MainApp.getAppContext().getContentResolver());

        OCFile ocFile = mFileDataStorageManager.getFileByPath(uri.getPath());
        return ocFile;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        OCFile ocFile = getFile(uri);

        Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                String.valueOf("r" + ocFile.getRemoteId()));

        // create a file to write bitmap data
        File f = new File(MainApp.getAppContext().getCacheDir(), ocFile.getFileName());
        try {
            f.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            fos.write(bitmapdata);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        OCFile ocFile = getFile(uri);
        return ocFile.getMimetype();
    }

    @Override
    public Cursor query(Uri uri, String[] arg1, String arg2, String[] arg3, String arg4) {
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
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
