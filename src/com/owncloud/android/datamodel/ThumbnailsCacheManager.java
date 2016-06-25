/**
 *   ownCloud Android client application
 *
 *   @author Tobias Kaminsky
 *   @author David A. Velasco
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
 */

package com.owncloud.android.datamodel;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import android.accounts.Account;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.adapter.DiskLruImageCache;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;

/**
 * Manager for concurrent access to thumbnails cache.
 */
public class ThumbnailsCacheManager {
    
    private static final String TAG = ThumbnailsCacheManager.class.getSimpleName();
    
    private static final String CACHE_FOLDER = "thumbnailCache";

    private static final Object mThumbnailsDiskCacheLock = new Object();
    private static DiskLruImageCache mThumbnailCache = null;
    private static boolean mThumbnailCacheStarting = true;

    private static final int RUNTIME_MAX_MEMORY_KBYTE = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int MEMORY_LRU_CACHE_SIZE = RUNTIME_MAX_MEMORY_KBYTE / 4; // use an 8th of the availiable memory for cache

    private LruCache<String, Bitmap> fullSizeLruCache = new LruCache<String, Bitmap>(MEMORY_LRU_CACHE_SIZE);
    
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final CompressFormat mCompressFormat = CompressFormat.JPEG;
    private static final int mCompressQuality = 70;
    private static OwnCloudClient mClient = null;

    public static void GenerateThumbnail(OCFile file, ImageView fileIcon, FileDataStorageManager storageManager, Account account)
    {
        if(file == null || fileIcon == null)
            return;

        fileIcon.setImageBitmap(ThumbnailsCacheManager.mDefaultImg);

        DiskCacheRequest dcr = new DiskCacheRequest();
        dcr.file = file;
        dcr.fileIcon = fileIcon;
        dcr.storageManager = storageManager;
        dcr.account = account;

        for(DiskCacheRequest r : diskCacheRequestQueue)
        {
            if(r.fileIcon == dcr.fileIcon) {
                Log_OC.d(TAG, "removing for reused ImageView");
                diskCacheRequestQueue.remove(r);
                break;
            }
        }

        diskCacheRequestQueue.put(dcr);

        // kick of thread if not already running or stopped
        if(!cacheIOThread.isAlive() && !cacheIOThread.isInterrupted()) {
            cacheIOThread.start();
        }
    }

    private static Bitmap handlePNG(Bitmap bitmap, int px){
        Bitmap resultBitmap = Bitmap.createBitmap(px,
                px,
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(resultBitmap);

        c.drawColor(MainApp.getAppContext().getResources().
                getColor(R.color.background_color));
        c.drawBitmap(bitmap, 0, 0, null);

        return resultBitmap;
    }

    private static Bitmap getOCThumb(OCFile file, OwnCloudClient client, Account account) {
        final String imageKey = String.valueOf(file.getRemoteId());

        Bitmap thumbnail = null;

        Log_OC.d(TAG, "NOT FOUND DOWNLOADING!");
        // Not found in disk cache
        if (file.needsUpdateThumbnail() || true) {
            Resources r = MainApp.getAppContext().getResources();
            int px = Math.round(r.getDimension(R.dimen.file_icon_size_grid));

            // Download thumbnail from server
            OwnCloudVersion serverOCVersion = AccountUtils.getServerVersion(account);
            if (client != null && serverOCVersion != null) {
                if (serverOCVersion.supportsRemoteThumbnails()) {
                    GetMethod get = null;
                    try {
                        String uri = client.getBaseUri() + "" +
                                "/index.php/apps/files/api/v1/thumbnail/" +
                                px + "/" + px + Uri.encode(file.getRemotePath(), "/");
                        Log_OC.d("Thumbnail", "URI: " + uri);
                        get = new GetMethod(uri);
                        get.setRequestHeader("Cookie",
                                "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");
                        int status = client.executeMethod(get);
                        if (status == HttpStatus.SC_OK) {
                            InputStream inputStream = get.getResponseBodyAsStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);

                            // Handle PNG
                            if (file.getMimetype().equalsIgnoreCase("image/png")) {
                                thumbnail = handlePNG(thumbnail, px);
                            }

                            // Add thumbnail to cache
                            if (thumbnail != null) {
                                addBitmapToCache(imageKey, thumbnail);
                            }
                        } else {
                            client.exhaustResponse(get.getResponseBodyAsStream());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (get != null) {
                            get.releaseConnection();
                        }
                    }
                } else {
                    Log_OC.d(TAG, "Server too old");
                }
            }
        }

        return thumbnail;

    }

    private static class DiskCacheRequest implements Comparable<DiskCacheRequest>
    {
        protected Integer priority = 5;

        protected OCFile file;
        protected ImageView fileIcon;
        protected FileDataStorageManager storageManager;
        protected Account account;

        @Override
        public int compareTo(DiskCacheRequest c) {
            return this.priority.compareTo(c.priority);
        }
    }

    private static PriorityBlockingQueue<DiskCacheRequest> diskCacheRequestQueue = new PriorityBlockingQueue<>();

    private static Thread cacheIOThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    final DiskCacheRequest dcr = diskCacheRequestQueue.take();

                    Log_OC.d(TAG, "processing disk cache request on second thread!");

                    Bitmap thumbnail = null;

                    try {
                        // try cache
                        String cacheKey = String.valueOf(dcr.file.getRemoteId());

                        Log_OC.d(TAG, "CacheKey is " + cacheKey);
                        if (cacheKey != null) {
                            thumbnail = getBitmapFromDiskCache(cacheKey);
                        }

                        if(thumbnail == null) {
                            if (dcr.file instanceof OCFile) {
                                if (dcr.account != null) {
                                    OwnCloudAccount ocAccount = new OwnCloudAccount(dcr.account,
                                            MainApp.getAppContext());
                                    OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().
                                            getClientFor(ocAccount, MainApp.getAppContext());
                                    thumbnail = getOCThumb(dcr.file, client, dcr.account);
                                }
                            }
                            // TODO: Add normal File

                            addBitmapToCache(cacheKey, thumbnail);
                        }

                        if(thumbnail != null) {
                            final Bitmap thumbCopy = thumbnail;

                            // do on handler
                            Log_OC.d(TAG, "Setting preview image via UI Thread Handler!");
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    final ImageView imageView = dcr.fileIcon;
                                    imageView.setImageBitmap(thumbCopy);
                                }
                            });
                        }
                    }catch(Throwable t){
                        // the app should never break due to a problem with thumbnails
                        Log_OC.e(TAG, "Generation of thumbnail for " + dcr.file + " failed", t);
                        if (t instanceof OutOfMemoryError) {
                            System.gc();
                        }
                    }
                }
            }
            catch(InterruptedException e) { /* expected behaviour on application exit */ }
        }
    });

    public static Bitmap mDefaultImg = 
            BitmapFactory.decodeResource(
                    MainApp.getAppContext().getResources(),
                    R.drawable.file_image
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
                } catch (InterruptedException e) {
                    Log_OC.e(TAG, "Wait in mThumbnailsDiskCacheLock was interrupted", e);
                }
            }
            if (mThumbnailCache != null) {
                return mThumbnailCache.getBitmap(key);
            }
        }
        return null;
    }

    public static class ThumbnailGenerationTask extends AsyncTask<Object, Bitmap, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private static Account mAccount;
        private Object mFile;
        private FileDataStorageManager mStorageManager;


        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager,
                                       Account account) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<ImageView>(imageView);
            if (storageManager == null)
                throw new IllegalArgumentException("storageManager must not be NULL");
            mStorageManager = storageManager;
            mAccount = account;
        }

        public ThumbnailGenerationTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap thumbnail = null;

            try {
                mFile = params[0];

                // try cache
                String cacheKey = null;
                if (mFile instanceof OCFile) {
                    cacheKey = ((OCFile) mFile).getRemoteId();
                } else if (mFile instanceof File) {
                    cacheKey = String.valueOf(((File) mFile).hashCode());
                }
                Log_OC.d(TAG, "CacheKey is " + cacheKey);
                if (cacheKey != null) {
                    thumbnail = getBitmapFromDiskCache(cacheKey);
                }
                if (thumbnail != null) {
                    Log_OC.d(TAG, "Early cache hit in ThumbnailGenerationTask");
                    return thumbnail;
                }

                if (mAccount != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount,
                            MainApp.getAppContext());
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                }

                if (mFile instanceof OCFile) {
                    thumbnail = doOCFileInBackground();
                } else if (mFile instanceof File) {
                    thumbnail = doFileInBackground();
                    //} else {  do nothing
                }

                addBitmapToCache(cacheKey, thumbnail);
                }catch(Throwable t){
                    // the app should never break due to a problem with thumbnails
                    Log_OC.e(TAG, "Generation of thumbnail for " + mFile + " failed", t);
                    if (t instanceof OutOfMemoryError) {
                        System.gc();
                    }
                }

            //Bitmap preview = Bitmap.createScaledBitmap(thumbnail, 10,10, true);

            // publishProgress(preview);

            return thumbnail;
        }

        protected void onPostExecute(Bitmap bitmap){
            if (bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask) {
                    String tagId = "";
                    if (mFile instanceof OCFile){
                        tagId = String.valueOf(((OCFile)mFile).getFileId());
                    } else if (mFile instanceof File){
                        tagId = String.valueOf(mFile.hashCode());
                    }
                    if (String.valueOf(imageView.getTag()).equals(tagId)) {
                        final ThumbnailsCacheManager.AsyncDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncDrawable(
                                        imageView.getResources(),
                                        bitmap,
                                        this
                                );
                        imageView.setImageDrawable(asyncDrawable);
                        // imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }

        /**
         * Add thumbnail to cache
         * @param imageKey: thumb key
         * @param bitmap:   image for extracting thumbnail
         * @param path:     image path
         * @param px:       thumbnail dp
         * @return Bitmap
         */
        private Bitmap addThumbnailToCache(String imageKey, Bitmap bitmap, String path, int px){

            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);

            // Rotate image, obeying exif tag
            thumbnail = BitmapUtils.rotateImage(thumbnail,path);

            // Add thumbnail to cache
            addBitmapToCache(imageKey, thumbnail);

            return thumbnail;
        }

        /**
         * Converts size of file icon from dp to pixel
         * @return int
         */
        private int getThumbnailDimension(){
            // Converts dp to pixel
            Resources r = MainApp.getAppContext().getResources();
            return Math.round(r.getDimension(R.dimen.file_icon_size_grid));
        }

        private Bitmap doOCFileInBackground() {
            OCFile file = (OCFile)mFile;

            final String imageKey = String.valueOf(file.getRemoteId());

            Log_OC.d(TAG, "doOCFileInBackground CacheKEy " + imageKey);

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null || file.needsUpdateThumbnail()) {

                int px = getThumbnailDimension();

                if (file.isDown()) {
                    Bitmap temp = BitmapUtils.decodeSampledBitmapFromFile(
                            file.getStoragePath(), px, px);
                    Bitmap bitmap = ThumbnailUtils.extractThumbnail(temp, px, px);

                    if (bitmap != null) {
                        // Handle PNG
                        if (file.getMimetype().equalsIgnoreCase("image/png")) {
                            bitmap = handlePNG(bitmap, px);
                        }

                        thumbnail = addThumbnailToCache(imageKey, bitmap, file.getStoragePath(), px);

                        file.setNeedsUpdateThumbnail(false);
                        mStorageManager.saveFile(file);
                    }

                } else {
                    // Download thumbnail from server
                    OwnCloudVersion serverOCVersion = AccountUtils.getServerVersion(mAccount);
                    if (mClient != null && serverOCVersion != null) {
                        if (serverOCVersion.supportsRemoteThumbnails()) {
                            GetMethod get = null;
                            try {
                                String uri = mClient.getBaseUri() + "" +
                                        "/index.php/apps/files/api/v1/thumbnail/" +
                                        px + "/" + px + Uri.encode(file.getRemotePath(), "/");
                                Log_OC.d("Thumbnail", "URI: " + uri);
                                get = new GetMethod(uri);
                                get.setRequestHeader("Cookie",
                                        "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");
                                int status = mClient.executeMethod(get);
                                if (status == HttpStatus.SC_OK) {
                                    InputStream inputStream = get.getResponseBodyAsStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);

                                    // Handle PNG
                                    if (file.getMimetype().equalsIgnoreCase("image/png")) {
                                        thumbnail = handlePNG(thumbnail, px);
                                    }

                                    // Add thumbnail to cache
                                    if (thumbnail != null) {
                                        addBitmapToCache(imageKey, thumbnail);
                                    }
                                } else {
                                    mClient.exhaustResponse(get.getResponseBodyAsStream());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (get != null) {
                                    get.releaseConnection();
                                }
                            }
                        } else {
                            Log_OC.d(TAG, "Server too old");
                        }
                    }
                }
            }

            return thumbnail;

        }

        private Bitmap handlePNG(Bitmap bitmap, int px){
            Bitmap resultBitmap = Bitmap.createBitmap(px,
                    px,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(resultBitmap);

            c.drawColor(MainApp.getAppContext().getResources().
                    getColor(R.color.background_color));
            c.drawBitmap(bitmap, 0, 0, null);

            return resultBitmap;
        }

        private Bitmap doFileInBackground() {
            File file = (File)mFile;

            final String imageKey = String.valueOf(file.hashCode());

            Log_OC.d(TAG, "doFileInBackground CacheKEy " + imageKey);
            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) {

                int px = getThumbnailDimension();

                Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(
                        file.getAbsolutePath(), px, px);

                if (bitmap != null) {
                    thumbnail = addThumbnailToCache(imageKey, bitmap, file.getPath(), px);
                }
            }
            return thumbnail;
        }

    }

    public static boolean cancelPotentialWork(Object file, ImageView imageView) {
        final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.mFile;
            // If bitmapData is not yet set or it differs from the new data

            if (bitmapData == null || bitmapData != file) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
                Log_OC.v(TAG, "Cancelled generation of thumbnail for a reused imageView");
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

    public static class AsyncDrawable extends BitmapDrawable {
        private final ThumbnailGenerationTask bitmapWorkerTaskReference;

        public AsyncDrawable(
                Resources res, Bitmap bitmap, ThumbnailGenerationTask bitmapWorkerTask
        ) {

            super(res, bitmap);
            bitmapWorkerTaskReference =
                    bitmapWorkerTask;
        }

        public ThumbnailGenerationTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference;
        }
    }
}
