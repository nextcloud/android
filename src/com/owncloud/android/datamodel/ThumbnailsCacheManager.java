/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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
 */

package com.owncloud.android.datamodel;

import java.io.File;
import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.widget.ImageView;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.DiskLruImageCache;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;

/**
 * Manager for concurrent access to thumbnails cache. 
 *  
 * @author Tobias Kaminsky
 * @author David A. Velasco
 */
public class ThumbnailsCacheManager {
    
    private static final String TAG = ThumbnailsCacheManager.class.getSimpleName();
    
    private static final String CACHE_FOLDER = "thumbnailCache"; 
    
    private static final Object mThumbnailsDiskCacheLock = new Object();
    private static DiskLruImageCache mThumbnailCache = null;
    private static boolean mThumbnailCacheStarting = true;
    
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final CompressFormat mCompressFormat = CompressFormat.JPEG;
    private static final int mCompressQuality = 70;
    
    public static Bitmap mDefaultImg = 
            BitmapFactory.decodeResource(
                    MainApp.getAppContext().getResources(), 
                    DisplayUtils.getResourceId("image/png", "default.png")
            );

    
    public static class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mThumbnailsDiskCacheLock) {
                mThumbnailCacheStarting = true;
                if (mThumbnailCache == null) {
                    try {
                        // Check if media is mounted or storage is built-in, if so, 
                        // try and use external cache dir; otherwise use internal cache dir
                        final String cachePath = 
                                MainApp.getAppContext().getExternalCacheDir().getPath() + 
                                File.separator + CACHE_FOLDER;
                        Log_OC.d(TAG, "create dir: " + cachePath);
                        final File diskCacheDir = new File(cachePath);
                        mThumbnailCache = new DiskLruImageCache(
                                diskCacheDir, 
                                DISK_CACHE_SIZE, 
                                mCompressFormat, 
                                mCompressQuality
                        );
                    } catch (Exception e) {
                        Log_OC.d(TAG, "Thumbnail cache could not be opened ", e);
                        mThumbnailCache = null;
                    }
                }
                mThumbnailCacheStarting = false; // Finished initialization
                mThumbnailsDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }
    
    
    public static void addBitmapToCache(String key, Bitmap bitmap) {
        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache != null) {
                mThumbnailCache.put(key, bitmap);
            }
        }
    }


    public static Bitmap getBitmapFromDiskCache(String key) {
        synchronized (mThumbnailsDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mThumbnailCacheStarting) {
                try {
                    mThumbnailsDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            if (mThumbnailCache != null) {
                return (Bitmap) mThumbnailCache.getBitmap(key);
            }
        }
        return null;
    }

    
    public static boolean cancelPotentialWork(OCFile file, ImageView imageView) {
        final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final OCFile bitmapData = bitmapWorkerTask.mFile;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || bitmapData != file) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }
    
    public static ThumbnailGenerationTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
         }
         return null;
     }

    public static class ThumbnailGenerationTask extends AsyncTask<OCFile, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private OCFile mFile;
        private FileDataStorageManager mStorageManager;
        
        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager) {
         // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<ImageView>(imageView);
            if (storageManager == null)
                throw new IllegalArgumentException("storageManager must not be NULL");
            mStorageManager = storageManager;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(OCFile... params) {
            Bitmap thumbnail = null;
            
            try {
                mFile = params[0];
                final String imageKey = String.valueOf(mFile.getRemoteId());
    
                // Check disk cache in background thread
                thumbnail = getBitmapFromDiskCache(imageKey);
    
                // Not found in disk cache
                if (thumbnail == null || mFile.needsUpdateThumbnail()) { 
                    // Converts dp to pixel
                    Resources r = MainApp.getAppContext().getResources();
                    int px = (int) Math.round(TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 150, r.getDisplayMetrics()
                    ));
                    
                    if (mFile.isDown()){
                        Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(
                                mFile.getStoragePath(), px, px);
                        
                        if (bitmap != null) {
                            thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);
    
                            // Add thumbnail to cache
                            addBitmapToCache(imageKey, thumbnail);

                            mFile.setNeedsUpdateThumbnail(false);
                            mStorageManager.saveFile(mFile);
                        }
    
                    }
                }
                
            } catch (Throwable t) {
                // the app should never break due to a problem with thumbnails
                Log_OC.e(TAG, "Generation of thumbnail for " + mFile + " failed", t);
                if (t instanceof OutOfMemoryError) {
                    System.gc();
                }
            }
            
            return thumbnail;
        }
        
        protected void onPostExecute(Bitmap bitmap){
            if (isCancelled()) {
                bitmap = null;
            }

            if (mImageViewReference != null && bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final ThumbnailGenerationTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    if (imageView.getTag().equals(mFile.getFileId())) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }
  
    
    public static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ThumbnailGenerationTask> bitmapWorkerTaskReference;

        public AsyncDrawable(
                Resources res, Bitmap bitmap, ThumbnailGenerationTask bitmapWorkerTask
            ) {
            
            super(res, bitmap);
            bitmapWorkerTaskReference =
                new WeakReference<ThumbnailGenerationTask>(bitmapWorkerTask);
        }

        public ThumbnailGenerationTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    
    /**
     * Remove from cache the remoteId passed
     * @param fileRemoteId: remote id of mFile passed
     */
    public static void removeFileFromCache(String fileRemoteId){
        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache != null) {
                mThumbnailCache.removeKey(fileRemoteId);
            }
            mThumbnailsDiskCacheLock.notifyAll(); // Wake any waiting threads
        }
    }

}
