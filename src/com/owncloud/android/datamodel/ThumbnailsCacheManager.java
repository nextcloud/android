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

import android.accounts.Account;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * Manager for concurrent access to thumbnails cache.
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
    private static OwnCloudClient mClient = null;

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

    public static class ThumbnailGenerationTask extends AsyncTask<Object, Void, Bitmap> {
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
                if (mAccount != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount,
                            MainApp.getAppContext());
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                }

                mFile = params[0];
                
                if (mFile instanceof OCFile) {
                    thumbnail = doOCFileInBackground();
                }  else if (mFile instanceof File) {
                    thumbnail = doFileInBackground();
                //} else {  do nothing
                }

                }catch(Throwable t){
                    // the app should never break due to a problem with thumbnails
                    Log_OC.e(TAG, "Generation of thumbnail for " + mFile + " failed", t);
                    if (t instanceof OutOfMemoryError) {
                        System.gc();
                    }
                }

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
                        imageView.setImageBitmap(bitmap);
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

    public static class AvatarGenerationTask extends AsyncTask<Object, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private static Account mAccount;
        private Object mUsername;
        private FileDataStorageManager mStorageManager;


        public AvatarGenerationTask(ImageView imageView, FileDataStorageManager storageManager,
                                       Account account) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<ImageView>(imageView);
            if (storageManager == null)
                throw new IllegalArgumentException("storageManager must not be NULL");
            mStorageManager = storageManager;
            mAccount = account;
        }

        public AvatarGenerationTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap thumbnail = null;

            try {
                if (mAccount != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount,
                            MainApp.getAppContext());
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                }

                mUsername = params[0];

                if (mUsername instanceof String) {
                    thumbnail = doAvatarInBackground();
                }

            }catch(Throwable t){
                // the app should never break due to a problem with avatars
                Log_OC.e(TAG, "Generation of avatar for " + mUsername + " failed", t);
                if (t instanceof OutOfMemoryError) {
                    System.gc();
                }
            }

            return thumbnail;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final AvatarGenerationTask avatarWorkerTask = getAvatarWorkerTask(imageView);
                if (this == avatarWorkerTask) {
                    String tagId = "";
                    if (mUsername instanceof String) {
                        tagId = (String) mUsername;
                        if (String.valueOf(imageView.getTag()).equals(tagId)) {
                            imageView.setImageBitmap(bitmap);
                        }
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
        private int getAvatarDimension(){
            // Converts dp to pixel
            Resources r = MainApp.getAppContext().getResources();
            return Math.round(r.getDimension(R.dimen.file_avatar_size));
        }

        private Bitmap doAvatarInBackground() {
            String username = (String) mUsername;

            final String imageKey = "a_" + username;

            // Check disk cache in background thread
            Bitmap avatar = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (avatar == null) {

                int px = getAvatarDimension();

                // Download avatar from server
                OwnCloudVersion serverOCVersion = AccountUtils.getServerVersion(mAccount);
                if (mClient != null && serverOCVersion != null) {
                    if (serverOCVersion.supportsRemoteThumbnails()) {
                        GetMethod get = null;
                        try {
                            String uri = mClient.getBaseUri() + "" +
                                    "/index.php/avatar/" + username + "/" + px;
                            Log_OC.d("Avatar", "URI: " + uri);
                            get = new GetMethod(uri);
                            int status = mClient.executeMethod(get);
                            if (status == HttpStatus.SC_OK) {
                                InputStream inputStream = get.getResponseBodyAsStream();
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                avatar = ThumbnailUtils.extractThumbnail(bitmap, px, px);

                                // Add avatar to cache
                                if (avatar != null) {
                                    addBitmapToCache(imageKey, avatar);
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
            return avatar;
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
            File file = (File) mUsername;

            final String imageKey = String.valueOf(file.hashCode());

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) {

                int px = getAvatarDimension();

                Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(
                        file.getAbsolutePath(), px, px);

                if (bitmap != null) {
                    thumbnail = addThumbnailToCache(imageKey, bitmap, file.getPath(), px);
                }
            }
            return thumbnail;
        }

    }

    public static boolean cancelPotentialThumbnailWork(Object file, ImageView imageView) {
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

    public static boolean cancelPotentialAvatarWork(Object file, ImageView imageView) {
        final AvatarGenerationTask avatarWorkerTask = getAvatarWorkerTask(imageView);

        if (avatarWorkerTask != null) {
            final Object usernameData = avatarWorkerTask.mUsername;
            // If usernameData is not yet set or it differs from the new data
            if (usernameData == null || usernameData != file) {
                // Cancel previous task
                avatarWorkerTask.cancel(true);
                Log_OC.v(TAG, "Cancelled generation of avatar for a reused imageView");
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
            if (drawable instanceof AsyncThumbnailDrawable) {
                final AsyncThumbnailDrawable asyncDrawable = (AsyncThumbnailDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static AvatarGenerationTask getAvatarWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncAvatarDrawable) {
                final AsyncAvatarDrawable asyncDrawable = (AsyncAvatarDrawable) drawable;
                return asyncDrawable.getAvatarWorkerTask();
            }
        }
        return null;
    }

    public static class AsyncThumbnailDrawable extends BitmapDrawable {
        private final WeakReference<ThumbnailGenerationTask> bitmapWorkerTaskReference;

        public AsyncThumbnailDrawable(
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

    public static class AsyncAvatarDrawable extends BitmapDrawable {
        private final WeakReference<AvatarGenerationTask> avatarWorkerTaskReference;

        public AsyncAvatarDrawable(
                Resources res, Bitmap bitmap, AvatarGenerationTask avatarWorkerTask
        ) {

            super(res, bitmap);
            avatarWorkerTaskReference =
                    new WeakReference<AvatarGenerationTask>(avatarWorkerTask);
        }

        public AvatarGenerationTask getAvatarWorkerTask() {
            return avatarWorkerTaskReference.get();
        }
    }
}
