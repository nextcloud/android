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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

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

    public static final String AUTHORITY = "com.owncloud.imageCache.provider";

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Account account = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
        FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(account,
                MainApp.getAppContext().getContentResolver());

        OCFile ocFile = fileDataStorageManager.getFileByPath(uri.getPath());

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
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
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
