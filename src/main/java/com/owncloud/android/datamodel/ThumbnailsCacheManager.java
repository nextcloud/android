/*
 *   ownCloud Android client application
 *
 *   @author Tobias Kaminsky
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.adapter.DiskLruImageCache;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Manager for concurrent access to thumbnails cache.
 */
public final class ThumbnailsCacheManager {
    private static final int READ_TIMEOUT = 40000;
    private static final int CONNECTION_TIMEOUT = 5000;

    public static final String PREFIX_RESIZED_IMAGE = "r";
    public static final String PREFIX_THUMBNAIL = "t";

    private static final String TAG = ThumbnailsCacheManager.class.getSimpleName();
    private static final String PNG_MIMETYPE = "image/png";
    private static final String CACHE_FOLDER = "thumbnailCache";
    public static final String AVATAR = "avatar";
    private static final String AVATAR_TIMESTAMP = "avatarTimestamp";
    private static final String ETAG = "ETag";

    private static final Object mThumbnailsDiskCacheLock = new Object();
    private static DiskLruImageCache mThumbnailCache;
    private static boolean mThumbnailCacheStarting = true;

    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 200; // 200MB
    private static final CompressFormat mCompressFormat = CompressFormat.JPEG;
    private static final int mCompressQuality = 70;
    private static OwnCloudClient mClient;

    public static final Bitmap mDefaultImg = BitmapFactory.decodeResource(MainApp.getAppContext().getResources(),
            R.drawable.file_image);

    public static final Bitmap mDefaultVideo = BitmapFactory.decodeResource(MainApp.getAppContext().getResources(),
            R.drawable.file_movie);

    private ThumbnailsCacheManager() {
    }

    public static class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mThumbnailsDiskCacheLock) {
                mThumbnailCacheStarting = true;

                if (mThumbnailCache == null) {
                    try {
                        File cacheDir = MainApp.getAppContext().getCacheDir();

                        if (cacheDir == null) {
                            throw new FileNotFoundException("Thumbnail cache could not be opened");
                        }

                        String cachePath = cacheDir.getPath() + File.separator + CACHE_FOLDER;
                        Log_OC.d(TAG, "thumbnail cache dir: " + cachePath);
                        File diskCacheDir = new File(cachePath);

                        // migrate from external cache to internal cache
                        File oldCacheDir = MainApp.getAppContext().getExternalCacheDir();

                        if (oldCacheDir != null && oldCacheDir.exists()) {
                            String cacheOldPath = oldCacheDir.getPath() + File.separator + CACHE_FOLDER;
                            File diskOldCacheDir = new File(cacheOldPath);

                            FileStorageUtils.copyDirs(diskOldCacheDir, diskCacheDir);
                            FileStorageUtils.deleteRecursive(diskOldCacheDir);
                        }

                        mThumbnailCache = new DiskLruImageCache(diskCacheDir, DISK_CACHE_SIZE, mCompressFormat,
                                                                mCompressQuality);
                    } catch (Exception e) {
                        Log_OC.d(TAG, String.format(Locale.US, "Disk cache init failed: %s", e.getMessage()));
                        mThumbnailCache = null;
                    }
                }
                mThumbnailCacheStarting = false; // Finished initialization
                mThumbnailsDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }

    /**
     * Converts size of file icon from dp to pixel
     * @return int
     */
    private static int getThumbnailDimension(){
        // Converts dp to pixel
        Resources r = MainApp.getAppContext().getResources();
        return Math.round(r.getDimension(R.dimen.file_icon_size_grid));
    }

    /**
     * Converts dimension of screen as point
     *
     * @return Point
     */
    private static Point getScreenDimension() {
        WindowManager wm = (WindowManager) MainApp.getAppContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point;
    }

    /**
     * Add thumbnail to cache
     * @param imageKey: thumb key
     * @param bitmap:   image for extracting thumbnail
     * @param path:     image path
     * @param pxW:      thumbnail width in pixel
     * @param pxH:      thumbnail height in pixel
     * @return Bitmap
     */
    private static Bitmap addThumbnailToCache(String imageKey, Bitmap bitmap, String path, int pxW, int pxH){

        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, pxW, pxH);

        // Rotate image, obeying exif tag
        thumbnail = BitmapUtils.rotateImage(thumbnail,path);

        // Add thumbnail to cache
        addBitmapToCache(imageKey, thumbnail);

        return thumbnail;
    }

    public static void addBitmapToCache(String key, Bitmap bitmap) {
        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache != null) {
                mThumbnailCache.put(key, bitmap);
            }
        }
    }

    public static boolean containsBitmap(String key) {
        return mThumbnailCache.containsKey(key);
    }

    public static Bitmap getScaledBitmapFromDiskCache(String key, int width, int height) {
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
                return mThumbnailCache.getScaledBitmap(key, width, height);
            }
        }
        return null;
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

    public static class ResizedImageGenerationTask extends AsyncTask<Object, Void, Bitmap> {
        private FileFragment fileFragment;
        private FileDataStorageManager storageManager;
        private Account account;
        private WeakReference<ImageView> imageViewReference;
        private WeakReference<FrameLayout> frameLayoutReference;
        private OCFile file;
        private ConnectivityService connectivityService;
        private int backgroundColor;


        public ResizedImageGenerationTask(FileFragment fileFragment,
                                          ImageView imageView,
                                          FrameLayout emptyListProgress,
                                          FileDataStorageManager storageManager,
                                          ConnectivityService connectivityService,
                                          Account account,
                                          int backgroundColor)
                throws IllegalArgumentException {
            this.fileFragment = fileFragment;
            imageViewReference = new WeakReference<>(imageView);
            frameLayoutReference = new WeakReference<>(emptyListProgress);
            this.storageManager = storageManager;
            this.connectivityService = connectivityService;
            this.account = account;
            this.backgroundColor = backgroundColor;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap thumbnail = null;

            file = (OCFile) params[0];

            try {
                if (account != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(account, MainApp.getAppContext());
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount,
                                                                                              MainApp.getAppContext());
                }

                thumbnail = doResizedImageInBackground();

                if (MimeTypeUtil.isVideo(file) && thumbnail != null) {
                    thumbnail = addVideoOverlay(thumbnail);
                }

            } catch (OutOfMemoryError oome) {
                Log_OC.e(TAG, "Out of memory");
            } catch (Throwable t) {
                // the app should never break due to a problem with thumbnails
                Log_OC.e(TAG, "Generation of thumbnail for " + file + " failed", t);
            }

            return thumbnail;
        }

        private Bitmap doResizedImageInBackground() {
            Bitmap thumbnail;

            String imageKey = PREFIX_RESIZED_IMAGE + file.getRemoteId();

            // Check disk cache in background thread
            thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null || file.isUpdateThumbnailNeeded()) {
                Point p = getScreenDimension();
                int pxW = p.x;
                int pxH = p.y;

                if (file.isDown()) {
                    Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.getStoragePath(), pxW, pxH);

                    if (bitmap != null) {
                        // Handle PNG
                        if (PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                            bitmap = handlePNG(bitmap, pxW, pxH);
                        }

                        thumbnail = addThumbnailToCache(imageKey, bitmap, file.getStoragePath(), pxW, pxH);

                        file.setUpdateThumbnailNeeded(false);
                        storageManager.saveFile(file);
                    }

                } else {
                    // Download thumbnail from server
                    if (mClient != null) {
                        GetMethod getMethod = null;
                        try {
                            String uri = mClient.getBaseUri() + "/index.php/core/preview.png?file="
                                + URLEncoder.encode(file.getRemotePath())
                                    + "&x=" + pxW + "&y=" + pxH + "&a=1&mode=cover&forceIcon=0";
                            getMethod = new GetMethod(uri);

                            int status = mClient.executeMethod(getMethod);
                            if (status == HttpStatus.SC_OK) {
                                InputStream inputStream = getMethod.getResponseBodyAsStream();
                                thumbnail = BitmapFactory.decodeStream(inputStream);
                            } else {
                                mClient.exhaustResponse(getMethod.getResponseBodyAsStream());
                            }

                                // Handle PNG
                                if (thumbnail != null && PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                                    thumbnail = handlePNG(thumbnail, thumbnail.getWidth(), thumbnail.getHeight());
                                }

                            // Add thumbnail to cache
                            if (thumbnail != null) {
                                Log_OC.d(TAG, "add thumbnail to cache: " + file.getFileName());
                                addBitmapToCache(imageKey, thumbnail);
                            }

                        } catch (Exception e) {
                            Log_OC.d(TAG, e.getMessage(), e);
                        } finally {
                            if (getMethod != null) {
                                getMethod.releaseConnection();
                            }
                        }
                    }
                }
            }

            return thumbnail;

        }

        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null) {
                final ImageView imageView = imageViewReference.get();
                final FrameLayout frameLayout = frameLayoutReference.get();

                if (bitmap != null) {
                    final ResizedImageGenerationTask bitmapWorkerTask = getResizedImageGenerationWorkerTask(imageView);

                    if (this == bitmapWorkerTask) {
                        String tagId = String.valueOf(file.getFileId());

                        if (String.valueOf(imageView.getTag()).equals(tagId)) {
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImageBitmap(bitmap);
                            imageView.setBackgroundColor(backgroundColor);

                            if (frameLayout != null) {
                                frameLayout.setVisibility(View.GONE);
                            }
                        }
                    }
                } else {
                    new Thread(() -> {
                        if (connectivityService.isInternetWalled()) {
                            if (fileFragment instanceof PreviewImageFragment) {
                                ((PreviewImageFragment) fileFragment).setNoConnectionErrorMessage();
                            }
                        } else {
                            if (fileFragment instanceof PreviewImageFragment) {
                                ((PreviewImageFragment) fileFragment).setErrorPreviewMessage();
                            }
                        }
                    }).start();

                }
            }
        }
    }

    public static class ThumbnailGenerationTaskObject {
        private Object file;
        private String imageKey;

        public ThumbnailGenerationTaskObject(Object file, String imageKey) {
            this.file = file;
            this.imageKey = imageKey;
        }

        private Object getFile() {
            return file;
        }

        private String getImageKey() {
            return imageKey;
        }
    }

    public static class ThumbnailGenerationTask extends AsyncTask<ThumbnailGenerationTaskObject, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private static Account mAccount;
        private List<ThumbnailGenerationTask> mAsyncTasks;
        private Object mFile;
        private String mImageKey;
        private FileDataStorageManager mStorageManager;
        private GetMethod getMethod;
        private Listener mListener;
        private boolean gridViewEnabled = false;

        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager, Account account)
                throws IllegalArgumentException {
            this(imageView, storageManager, account, null);
        }

        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager,
                                       Account account, List<ThumbnailGenerationTask> asyncTasks)
                throws IllegalArgumentException {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
            if (storageManager == null) {
                throw new IllegalArgumentException("storageManager must not be NULL");
            }
            mStorageManager = storageManager;
            mAccount = account;
            mAsyncTasks = asyncTasks;
        }

        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager,
                                       Account account, List<ThumbnailGenerationTask> asyncTasks,
                                       boolean gridViewEnabled)
            throws IllegalArgumentException {
            this(imageView, storageManager, account, asyncTasks);
            this.gridViewEnabled = gridViewEnabled;
        }

        public GetMethod getGetMethod() {
            return getMethod;
        }

        public ThumbnailGenerationTask(FileDataStorageManager storageManager, Account account){
            if (storageManager == null) {
                throw new IllegalArgumentException("storageManager must not be NULL");
            }
            mStorageManager = storageManager;
            mAccount = account;
            mImageViewReference = null;
        }

        public ThumbnailGenerationTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
        }

        @SuppressFBWarnings("Dm")
        @Override
        protected Bitmap doInBackground(ThumbnailGenerationTaskObject... params) {
            Bitmap thumbnail = null;
            try {
                if (mAccount != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(
                            mAccount,
                            MainApp.getAppContext()
                    );
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                }

                ThumbnailGenerationTaskObject object = params[0];
                mFile = object.getFile();
                mImageKey = object.getImageKey();

                if (mFile instanceof ServerFileInterface) {
                    thumbnail = doThumbnailFromOCFileInBackground();

                    if (MimeTypeUtil.isVideo((ServerFileInterface) mFile) && thumbnail != null) {
                        thumbnail = addVideoOverlay(thumbnail);
                    }
                } else if (mFile instanceof File) {
                    thumbnail = doFileInBackground();

                    String url = ((File) mFile).getAbsolutePath();
                    String mMimeType = FileStorageUtils.getMimeTypeFromName(url);

                    if (MimeTypeUtil.isVideo(mMimeType) && thumbnail != null) {
                        thumbnail = addVideoOverlay(thumbnail);
                    }
                    //} else {  do nothing
                }

            } catch(OutOfMemoryError oome) {
                Log_OC.e(TAG, "Out of memory");
            } catch (Throwable t) {
                // the app should never break due to a problem with thumbnails
                Log_OC.e(TAG, "Generation of thumbnail for " + mFile + " failed", t);
            }

            return thumbnail;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && mImageViewReference != null) {
                final ImageView imageView = mImageViewReference.get();
                final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

                if (this == bitmapWorkerTask) {
                    String tagId = "";
                    if (mFile instanceof OCFile) {
                        tagId = String.valueOf(((OCFile)mFile).getFileId());
                    } else if (mFile instanceof File) {
                        tagId = String.valueOf(mFile.hashCode());
                    } else if (mFile instanceof TrashbinFile) {
                        tagId = String.valueOf(((TrashbinFile) mFile).getRemoteId());
                    }
                    if (String.valueOf(imageView.getTag()).equals(tagId)) {
                        if (gridViewEnabled) {
                            BitmapUtils.setRoundedBitmapForGridMode(bitmap, imageView);
                        } else {
                            BitmapUtils.setRoundedBitmap(bitmap, imageView);
                        }
                    }
                }

                if (mListener != null) {
                    mListener.onSuccess();
                }
            } else {
                if (mListener != null) {
                    mListener.onError();
                }
            }

            if (mAsyncTasks != null) {
                mAsyncTasks.remove(this);
            }
        }

        public void setListener(Listener listener){
            mListener = listener;
        }

        private Bitmap doThumbnailFromOCFileInBackground() {
            Bitmap thumbnail;
            ServerFileInterface file = (ServerFileInterface) mFile;
            String imageKey = PREFIX_THUMBNAIL + file.getRemoteId();

            // Check disk cache in background thread
            thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null || (file instanceof OCFile && ((OCFile) file).isUpdateThumbnailNeeded())) {
                int pxW;
                int pxH;
                pxW = pxH = getThumbnailDimension();

                if (file instanceof OCFile) {
                    OCFile ocFile = (OCFile) file;
                    if (ocFile.isDown()) {
                        Bitmap bitmap;
                        if (MimeTypeUtil.isVideo(ocFile)) {
                            bitmap = ThumbnailUtils.createVideoThumbnail(ocFile.getStoragePath(),
                                    MediaStore.Images.Thumbnails.MINI_KIND);
                        } else {
                            bitmap = BitmapUtils.decodeSampledBitmapFromFile(ocFile.getStoragePath(), pxW, pxH);
                        }

                        if (bitmap != null) {
                            // Handle PNG
                            if (PNG_MIMETYPE.equalsIgnoreCase(ocFile.getMimeType())) {
                                bitmap = handlePNG(bitmap, pxW, pxH);
                            }

                            thumbnail = addThumbnailToCache(imageKey, bitmap, ocFile.getStoragePath(), pxW, pxH);

                            ocFile.setUpdateThumbnailNeeded(false);
                            mStorageManager.saveFile(ocFile);
                        }
                    }
                }

                if (thumbnail == null) {
                    // check if resized version is available
                    String resizedImageKey = PREFIX_RESIZED_IMAGE + file.getRemoteId();
                    Bitmap resizedImage = getBitmapFromDiskCache(resizedImageKey);

                    if (resizedImage != null) {
                        thumbnail = ThumbnailUtils.extractThumbnail(resizedImage, pxW, pxH);
                    } else {
                        // Download thumbnail from server
                        if (mClient != null) {
                            getMethod = null;
                            try {
                                // thumbnail
                                String uri;
                                if (file instanceof OCFile) {
                                    uri = mClient.getBaseUri() + "/index.php/apps/files/api/v1/thumbnail/" +
                                        pxW + "/" + pxH + Uri.encode(file.getRemotePath(), "/");
                                } else {
                                    uri = mClient.getBaseUri() + "/index.php/apps/files_trashbin/preview?fileId=" +
                                            file.getLocalId() + "&x=" + pxW + "&y=" + pxH;
                                }

                                Log_OC.d(TAG, "generate thumbnail: " + file.getFileName() + " URI: " + uri);
                                getMethod = new GetMethod(uri);
                                getMethod.setRequestHeader("Cookie",
                                        "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");

                                getMethod.setRequestHeader(RemoteOperation.OCS_API_HEADER,
                                        RemoteOperation.OCS_API_HEADER_VALUE);

                                int status = mClient.executeMethod(getMethod, READ_TIMEOUT, CONNECTION_TIMEOUT);
                                if (status == HttpStatus.SC_OK) {
                                    InputStream inputStream = getMethod.getResponseBodyAsStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    thumbnail = ThumbnailUtils.extractThumbnail(bitmap, pxW, pxH);
                                } else {
                                    mClient.exhaustResponse(getMethod.getResponseBodyAsStream());
                                }

                                // Handle PNG
                                if (PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                                    thumbnail = handlePNG(thumbnail, pxW, pxH);
                                }
                            } catch (Exception e) {
                                Log_OC.d(TAG, e.getMessage(), e);
                            } finally {
                                if (getMethod != null) {
                                    getMethod.releaseConnection();
                                }
                            }
                        }
                    }

                    // Add thumbnail to cache
                    if (thumbnail != null) {
                        Log_OC.d(TAG, "add thumbnail to cache: " + file.getFileName());
                        addBitmapToCache(imageKey, thumbnail);
                    }
                }
            }

            return thumbnail;
        }

        /**
         * Converts size of file icon from dp to pixel
         *
         * @return int
         */
        private int getThumbnailDimension() {
            // Converts dp to pixel
            Resources r = MainApp.getAppContext().getResources();
            Double d = Math.pow(2, Math.floor(Math.log(r.getDimension(R.dimen.file_icon_size_grid)) / Math.log(2)));
            return d.intValue();
        }

        private Bitmap doFileInBackground() {
            File file = (File)mFile;

            final String imageKey;
            if (mImageKey != null) {
                imageKey = mImageKey;
            } else {
                imageKey = String.valueOf(file.hashCode());
            }

            // local file should always generate a thumbnail
            mImageKey = PREFIX_THUMBNAIL + mImageKey;

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) {
                int pxW;
                int pxH;
                pxW = pxH = getThumbnailDimension();

                Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.getAbsolutePath(), pxW, pxH);

                if (bitmap != null) {
                    thumbnail = addThumbnailToCache(imageKey, bitmap, file.getPath(), pxW, pxH);
                }
            }
            return thumbnail;
        }

        public interface Listener{
            void onSuccess();
            void onError();
        }

    }

    public static class MediaThumbnailGenerationTask extends AsyncTask<Object, Void, Bitmap> {

        private static final int IMAGE_KEY_PARAMS_LENGTH = 2;

        private enum Type {IMAGE, VIDEO}
        private final WeakReference<ImageView> mImageViewReference;
        private File mFile;
        private String mImageKey;
        private Context mContext;

        public MediaThumbnailGenerationTask(ImageView imageView, Context context) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
            mContext = context;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap thumbnail = null;

            try {
                if (params[0] instanceof File) {
                    mFile = (File) params[0];
                    if (params.length == IMAGE_KEY_PARAMS_LENGTH) {
                        mImageKey = (String) params[1];
                    }

                    if (MimeTypeUtil.isImage(mFile)) {
                        thumbnail = doFileInBackground(mFile, Type.IMAGE);
                    } else if (MimeTypeUtil.isVideo(mFile)) {
                        thumbnail = doFileInBackground(mFile, Type.VIDEO);
                    }
                }
            } // the app should never break due to a problem with thumbnails
            catch (OutOfMemoryError t) {
                Log_OC.e(TAG, "Generation of thumbnail for " + mFile.getAbsolutePath() + " failed", t);
                Log_OC.e(TAG, "Out of memory");
            } catch (Throwable t) {
                // the app should never break due to a problem with thumbnails
                Log_OC.e(TAG, "Generation of thumbnail for " + mFile.getAbsolutePath() + " failed", t);
            }

            return thumbnail;
        }

        protected void onPostExecute(Bitmap bitmap) {
            String tagId = "";
            final ImageView imageView = mImageViewReference.get();
            if (imageView != null) {
                if (mFile != null) {
                    tagId = String.valueOf(mFile.hashCode());
                }

                if (bitmap != null) {
                    if (tagId.equals(String.valueOf(imageView.getTag()))) {
                        imageView.setImageBitmap(bitmap);
                    }
                } else {
                    if (mFile != null) {
                        if (mFile.isDirectory()) {
                            imageView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(mContext));
                        } else {
                            if (MimeTypeUtil.isVideo(mFile)) {
                                imageView.setImageBitmap(ThumbnailsCacheManager.mDefaultVideo);
                            } else {
                                imageView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null, mFile.getName(),
                                        mContext));
                            }
                        }
                    }
                }
            }
        }

        private Bitmap doFileInBackground(File file, Type type) {
            final String imageKey;

            if (mImageKey != null) {
                imageKey = mImageKey;
            } else {
                imageKey = String.valueOf(file.hashCode());
            }

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) {

                if (Type.IMAGE.equals(type)) {
                    int px = getThumbnailDimension();

                    Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.getAbsolutePath(), px, px);

                    if (bitmap != null) {
                        thumbnail = addThumbnailToCache(imageKey, bitmap, file.getPath(), px, px);
                    }
                } else if (Type.VIDEO.equals(type)) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(file.getAbsolutePath());
                        thumbnail = retriever.getFrameAtTime(-1);
                    } catch (Exception ex) {
                        // can't create a bitmap
                        Log_OC.w(TAG, "Failed to create bitmap from video " + file.getAbsolutePath());
                    } finally {
                        try {
                            retriever.release();
                        } catch (RuntimeException ex) {
                            // Ignore failure at this point.
                            Log_OC.w(TAG, "Failed release MediaMetadataRetriever for " + file.getAbsolutePath());
                        }
                    }

                    if (thumbnail != null) {
                        // Scale down bitmap if too large.
                        int px = getThumbnailDimension();
                        int width = thumbnail.getWidth();
                        int height = thumbnail.getHeight();
                        int max = Math.max(width, height);
                        if (max > px) {
                            thumbnail = BitmapUtils.scaleBitmap(thumbnail, px, width, height, max);
                            thumbnail = addThumbnailToCache(imageKey, thumbnail, file.getPath(), px, px);
                        }
                    }
                }
            }

            return thumbnail;
        }
    }

    public static class AvatarGenerationTask extends AsyncTask<String, Void, Drawable> {
        private final WeakReference<AvatarGenerationListener> mAvatarGenerationListener;
        private final Object mCallContext;
        private final Resources mResources;
        private final float mAvatarRadius;
        private User user;
        private String mUserId;
        private String mServerName;
        private Context mContext;


        public AvatarGenerationTask(AvatarGenerationListener avatarGenerationListener,
                                    Object callContext,
                                    User user,
                                    Resources resources,
                                    float avatarRadius,
                                    String userId,
                                    String serverName,
                                    Context context) {
            mAvatarGenerationListener = new WeakReference<>(avatarGenerationListener);
            mCallContext = callContext;
            this.user = user;
            mResources = resources;
            mAvatarRadius = avatarRadius;
            mUserId = userId;
            mServerName = serverName;
            mContext = context;
        }

        @SuppressFBWarnings("Dm")
        @Override
        protected Drawable doInBackground(String... params) {
            Drawable thumbnail = null;

            try {
                thumbnail = doAvatarInBackground();
            } catch (OutOfMemoryError oome) {
                Log_OC.e(TAG, "Out of memory");
            } catch (Throwable t) {
                // the app should never break due to a problem with avatars
                thumbnail = ResourcesCompat.getDrawable(mResources, R.drawable.account_circle_white, null);
                Log_OC.e(TAG, "Generation of avatar for " + mUserId + " failed", t);
            }

            return thumbnail;
        }

        protected void onPostExecute(Drawable drawable) {
            if (drawable != null) {
                AvatarGenerationListener listener = mAvatarGenerationListener.get();
                if (listener != null) {
                    String accountName = mUserId + "@" + mServerName;
                    if (listener.shouldCallGeneratedCallback(accountName, mCallContext)) {
                        listener.avatarGenerated(drawable, mCallContext);
                    }
                }
            }
        }

        /**
         * Converts size of file icon from dp to pixel
         *
         * @return int
         */
        private int getAvatarDimension() {
            // Converts dp to pixel
            Resources r = MainApp.getAppContext().getResources();
            return Math.round(r.getDimension(R.dimen.file_avatar_size));
        }

        private @NonNull
        Drawable doAvatarInBackground() {
            Bitmap avatar;

            String accountName = mUserId + "@" + mServerName;

            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(mContext.getContentResolver());

            String eTag = arbitraryDataProvider.getValue(accountName, ThumbnailsCacheManager.AVATAR);
            long timestamp = arbitraryDataProvider.getLongValue(accountName, ThumbnailsCacheManager.AVATAR_TIMESTAMP);
            String avatarKey = "a_" + mUserId + "_" + mServerName + "_" + eTag;
            avatar = getBitmapFromDiskCache(avatarKey);

            // Download avatar from server, only if older than 60 min or avatar does not exist
            if (System.currentTimeMillis() - timestamp >= 60 * 60 * 1000 || avatar == null) {
                GetMethod get = null;
                try {
                    if (user != null) {
                        OwnCloudAccount ocAccount = user.toOwnCloudAccount();
                        mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, mContext);
                    }

                    int px = getAvatarDimension();
                    String uri = mClient.getBaseUri() + "/index.php/avatar/" + Uri.encode(mUserId) + "/" + px;
                    Log_OC.d("Avatar", "URI: " + uri);
                    get = new GetMethod(uri);

                    // only use eTag if available and corresponding avatar is still there
                    // (might be deleted from cache)
                    if (!eTag.isEmpty() && avatar != null) {
                        get.setRequestHeader("If-None-Match", eTag);
                    }

                    int status = mClient.executeMethod(get);

                    // we are using eTag to download a new avatar only if it changed
                    switch (status) {
                        case HttpStatus.SC_OK:
                        case HttpStatus.SC_CREATED:
						    // new avatar
                            InputStream inputStream = get.getResponseBodyAsStream();

                            String newETag = null;
                            if (get.getResponseHeader(ETAG) != null) {
                                newETag = get.getResponseHeader(ETAG).getValue().replace("\"", "");
                                arbitraryDataProvider.storeOrUpdateKeyValue(accountName, AVATAR, newETag);
                            }

                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            avatar = ThumbnailUtils.extractThumbnail(bitmap, px, px);

                            // Add avatar to cache
                            if (avatar != null && !TextUtils.isEmpty(newETag)) {
                                avatar = handlePNG(avatar, px, px);
                                String newImageKey = "a_" + mUserId + "_" + mServerName + "_" + newETag;
                                addBitmapToCache(newImageKey, avatar);
                                arbitraryDataProvider.storeOrUpdateKeyValue(accountName,
                                                                            ThumbnailsCacheManager.AVATAR_TIMESTAMP,
                                                                            System.currentTimeMillis());
                            } else {
                                return TextDrawable.createAvatar(user, mAvatarRadius);
                            }
                            break;

                        case HttpStatus.SC_NOT_MODIFIED:
                            // old avatar
                            mClient.exhaustResponse(get.getResponseBodyAsStream());
                            arbitraryDataProvider.storeOrUpdateKeyValue(accountName,
                                                                        ThumbnailsCacheManager.AVATAR_TIMESTAMP,
                                                                        System.currentTimeMillis());
                            break;
                        default:
                            // everything else
                            mClient.exhaustResponse(get.getResponseBodyAsStream());
                            break;
                    }
                } catch (Exception e) {
                    try {
                        return TextDrawable.createAvatar(user, mAvatarRadius);
                    } catch (Exception e1) {
                        Log_OC.e(TAG, "Error generating fallback avatar");
                    }
                } finally {
                    if (get != null) {
                        get.releaseConnection();
                    }
                }
            }

            if (avatar == null) {
                try {
                    return TextDrawable.createAvatar(user, mAvatarRadius);
                } catch (Exception e1) {
                    return ResourcesCompat.getDrawable(mResources, R.drawable.ic_user, null);
                }
            } else {
                return BitmapUtils.bitmapToCircularBitmapDrawable(mResources, avatar);
            }
        }
    }

    public static boolean cancelPotentialThumbnailWork(Object file, ImageView imageView) {
        final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.mFile;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || !bitmapData.equals(file)) {
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
            if (drawable instanceof AsyncThumbnailDrawable) {
                final AsyncThumbnailDrawable asyncDrawable = (AsyncThumbnailDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static ResizedImageGenerationTask getResizedImageGenerationWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncResizedImageDrawable) {
                final AsyncResizedImageDrawable asyncDrawable = (AsyncResizedImageDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static Bitmap addVideoOverlay(Bitmap thumbnail) {
        Drawable playButtonDrawable = ResourcesCompat.getDrawable(MainApp.getAppContext().getResources(),
                                                                  R.drawable.view_play,
                                                                  null);
        Bitmap playButton = BitmapUtils.drawableToBitmap(playButtonDrawable);

        Bitmap resizedPlayButton = Bitmap.createScaledBitmap(playButton,
                                                             (int) (thumbnail.getWidth() * 0.3),
                                                             (int) (thumbnail.getHeight() * 0.3), true);

        Bitmap resultBitmap = Bitmap.createBitmap(thumbnail.getWidth(),
                                                  thumbnail.getHeight(),
                                                  Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(resultBitmap);

        // compute visual center of play button, according to resized image
        int x1 = resizedPlayButton.getWidth();
        int y1 = resizedPlayButton.getHeight() / 2;
        int x2 = 0;
        int y2 = resizedPlayButton.getWidth();
        int x3 = 0;
        int y3 = 0;

        double ym = ( ((Math.pow(x3,2) - Math.pow(x1,2) + Math.pow(y3,2) - Math.pow(y1,2)) *
                (x2 - x1)) - (Math.pow(x2,2) - Math.pow(x1,2) + Math.pow(y2,2) -
                Math.pow(y1,2)) * (x3 - x1) )  /  (2 * ( ((y3 - y1) * (x2 - x1)) -
                ((y2 - y1) * (x3 - x1)) ));
        double xm = ( (Math.pow(x2,2) - Math.pow(x1,2)) + (Math.pow(y2,2) - Math.pow(y1,2)) -
                (2*ym*(y2 - y1)) ) / (2*(x2 - x1));

        // offset to top left
        double ox = - xm;


        c.drawBitmap(thumbnail, 0, 0, null);

        Paint p = new Paint();
        p.setAlpha(230);

        c.drawBitmap(resizedPlayButton, (float) ((thumbnail.getWidth() / 2) + ox),
                (float) ((thumbnail.getHeight() / 2) - ym), p);

        return resultBitmap;
    }

    public static class AsyncThumbnailDrawable extends BitmapDrawable {
        private final WeakReference<ThumbnailGenerationTask> bitmapWorkerTaskReference;

        public AsyncThumbnailDrawable(
                Resources res, Bitmap bitmap, ThumbnailGenerationTask bitmapWorkerTask
        ) {

            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public ThumbnailGenerationTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static class AsyncResizedImageDrawable extends BitmapDrawable {
        private final WeakReference<ResizedImageGenerationTask> bitmapWorkerTaskReference;

        public AsyncResizedImageDrawable(Resources res, Bitmap bitmap, ResizedImageGenerationTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        private ResizedImageGenerationTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static class AsyncMediaThumbnailDrawable extends BitmapDrawable {

        public AsyncMediaThumbnailDrawable(Resources res, Bitmap bitmap) {

            super(res, bitmap);
        }
    }

    /**
     * adapted from https://stackoverflow.com/a/8113368
     */
    private static Bitmap handlePNG(Bitmap source, int newWidth, int newHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dest);
        canvas.drawColor(MainApp.getAppContext().getResources().getColor(R.color.background_color_png));
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    public static void generateResizedImage(OCFile file) {
        Point p = getScreenDimension();
        int pxW = p.x;
        int pxH = p.y;
        String imageKey = PREFIX_RESIZED_IMAGE + file.getRemoteId();

        Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.getStoragePath(), pxW, pxH);

        if (bitmap != null) {
            // Handle PNG
            if (PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                bitmap = handlePNG(bitmap, pxW, pxH);
            }

            addThumbnailToCache(imageKey, bitmap, file.getStoragePath(), pxW, pxH);
        }
    }

    public static void generateThumbnailFromOCFile(OCFile file, Account account, Context context) {
        int pxW;
        int pxH;
        pxW = pxH = getThumbnailDimension();
        String imageKey = PREFIX_THUMBNAIL + file.getRemoteId();

        GetMethod getMethod = null;

        try {
            Bitmap thumbnail = null;

            OwnCloudClient client = mClient;
            if (client == null) {
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context);
            }

            String uri = client.getBaseUri() + "/index.php/apps/files/api/v1/thumbnail/" +
                pxW + "/" + pxH + Uri.encode(file.getRemotePath(), "/");

            Log_OC.d(TAG, "generate thumbnail: " + file.getFileName() + " URI: " + uri);
            getMethod = new GetMethod(uri);
            getMethod.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");

            getMethod.setRequestHeader(RemoteOperation.OCS_API_HEADER,
                                       RemoteOperation.OCS_API_HEADER_VALUE);

            int status = client.executeMethod(getMethod);
            if (status == HttpStatus.SC_OK) {
                InputStream inputStream = getMethod.getResponseBodyAsStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                thumbnail = ThumbnailUtils.extractThumbnail(bitmap, pxW, pxH);
            } else {
                client.exhaustResponse(getMethod.getResponseBodyAsStream());
            }

            // Add thumbnail to cache
            if (thumbnail != null) {
                // Handle PNG
                if (PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                    thumbnail = handlePNG(thumbnail, pxW, pxH);
                }

                Log_OC.d(TAG, "add thumbnail to cache: " + file.getFileName());
                addBitmapToCache(imageKey, thumbnail);
            }
        } catch (Exception e) {
            Log_OC.d(TAG, e.getMessage(), e);
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }
    }
}
