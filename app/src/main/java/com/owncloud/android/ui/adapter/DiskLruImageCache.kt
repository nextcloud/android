/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Unpublished <unpublished@users.noreply.github.com>
 * SPDX-FileCopyrightText: 2014-2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2016 Iskra Delta <iskradelta@no-reply.github.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014 Jose Antonio Barros Ramos <jabarros@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.jakewharton.disklrucache.DiskLruCache;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.BitmapUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DiskLruImageCache {

    private DiskLruCache mDiskCache;
    private CompressFormat mCompressFormat;
    private int mCompressQuality;
    private static final int CACHE_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final String CACHE_TEST_DISK = "cache_test_DISK_";

    private static final String TAG = DiskLruImageCache.class.getSimpleName();

    public DiskLruImageCache(File diskCacheDir, int diskCacheSize, CompressFormat compressFormat, int quality)
        throws IOException {
        mDiskCache = DiskLruCache.open(diskCacheDir, CACHE_VERSION, VALUE_COUNT, diskCacheSize);
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException {
        try (OutputStream out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)) {
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        }
    }

    public void put(String key, Bitmap data) {

        DiskLruCache.Editor editor = null;
        String validKey = convertToValidKey(key);
        try {
            editor = mDiskCache.edit(validKey);
            if (editor == null) {
                return;
            }

            if (writeBitmapToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
                if (BuildConfig.DEBUG) {
                    Log_OC.d(CACHE_TEST_DISK, "image put on disk cache " + validKey);
                }
            } else {
                editor.abort();
                if (BuildConfig.DEBUG) {
                    Log_OC.d(CACHE_TEST_DISK, "ERROR on: image put on disk cache " + validKey);
                }
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(CACHE_TEST_DISK, "ERROR on: image put on disk cache " + validKey);
            }
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ex) {
                Log_OC.d(TAG, "Error aborting editor", ex);
            }
        }
    }

    public Bitmap getScaledBitmap(String key, int width, int height) {
        Bitmap bitmap = null;
        String validKey = convertToValidKey(key);

        try (DiskLruCache.Snapshot snapshot = mDiskCache.get(validKey)) {
            if (snapshot == null) {
                return null;
            }

            InputStream inputStream = snapshot.getInputStream(0);
            if (inputStream != null) {
                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                try (BufferedInputStream buffIn = new BufferedInputStream(inputStream, IO_BUFFER_SIZE)) {
                    options.inScaled = true;
                    options.inPurgeable = true;
                    options.inPreferQualityOverSpeed = false;
                    options.inMutable = false;
                    options.inJustDecodeBounds = true;

                    BitmapFactory.decodeStream(buffIn, null, options);
                }

                try (DiskLruCache.Snapshot snapshot2 = mDiskCache.get(validKey)) {
                    inputStream = snapshot2.getInputStream(0);

                    try (BufferedInputStream buffIn = new BufferedInputStream(inputStream, IO_BUFFER_SIZE)) {
                        // Calculate inSampleSize
                        options.inSampleSize = BitmapUtils.calculateSampleFactor(options, width, height);

                        // Decode bitmap with inSampleSize set
                        options.inJustDecodeBounds = false;
                        bitmap = BitmapFactory.decodeStream(buffIn, null, options);
                    }
                }
            }
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage(), e);
        }

        if (BuildConfig.DEBUG) {
            Log_OC.d(CACHE_TEST_DISK, bitmap == null ? "not found" : "image read from disk " + validKey);
        }

        return bitmap;
    }

    public Bitmap getBitmap(String key) {
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        InputStream in = null;
        BufferedInputStream buffIn = null;
        String validKey = convertToValidKey(key);

        try {
            snapshot = mDiskCache.get(validKey);
            if (snapshot == null) {
                return null;
            }
            in = snapshot.getInputStream(0);
            if (in != null) {
                buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                bitmap = BitmapFactory.decodeStream(buffIn);
            }
        } catch (IOException e) {
            Log_OC.e(TAG, e.getMessage(), e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
            if (buffIn != null) {
                try {
                    buffIn.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, e.getMessage(), e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, e.getMessage(), e);
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Log_OC.d(CACHE_TEST_DISK, bitmap == null ? "not found" : "image read from disk " + validKey);
        }

        return bitmap;
    }

    public boolean containsKey(String key) {

        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        String validKey = convertToValidKey(key);
        try {
            snapshot = mDiskCache.get(validKey);
            contained = snapshot != null;
        } catch (IOException e) {
            Log_OC.d(TAG, e.getMessage(), e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return contained;

    }

    public void clearCache() {
        if (BuildConfig.DEBUG) {
            Log_OC.d(CACHE_TEST_DISK, "disk cache CLEARED");
        }
        try {
            mDiskCache.delete();
        } catch (IOException e) {
            Log_OC.d(TAG, e.getMessage(), e);
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }

    private String convertToValidKey(String key) {
        return Integer.toString(key.hashCode());
    }

    /**
     * Remove passed key from cache
     *
     * @param key
     */
    public void removeKey(String key) {
        String validKey = convertToValidKey(key);
        try {
            mDiskCache.remove(validKey);
            Log_OC.d(TAG, "removeKey from cache: " + validKey);
        } catch (IOException e) {
            Log_OC.d(TAG, e.getMessage(), e);
        }
    }
}
