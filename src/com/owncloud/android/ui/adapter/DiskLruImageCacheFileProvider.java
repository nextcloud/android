/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 Tobias Kaminsky
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   adapted from: http://stephendnicholas.com/archives/974
 *
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DiskLruImageCacheFileProvider extends ContentProvider {
    private static String TAG = FileDataStorageManager.class.getSimpleName();
    private FileDataStorageManager mFileDataStorageManager;

    public static final String AUTHORITY = "org.owncloud.beta.imageCache.provider";

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

        File f = new File(MainApp.getAppContext().getCacheDir(), ocFile.getFileName());

        try {
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf("r" + ocFile.getRemoteId()));

            // TODO download resized Image
            if (thumbnail == null) {
                ThumbnailsCacheManager.ThumbnailGenerationTask task =
                        new ThumbnailsCacheManager.ThumbnailGenerationTask();
                task.execute(ocFile, false);
                thumbnail = task.get(5l, TimeUnit.SECONDS);
            }

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (thumbnail != null) {
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bos);
                byte[] bitmapdata = bos.toByteArray();

                // create a file to write bitmap data
                f.createNewFile();

                //write the bytes in file
                FileOutputStream fos = null;
                fos = new FileOutputStream(f);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();
            } else {
                throw new FileNotFoundException();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
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
