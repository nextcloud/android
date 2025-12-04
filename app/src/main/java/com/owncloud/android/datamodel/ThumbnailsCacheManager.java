/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022-2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016-2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.datamodel;

import android.annotation.SuppressLint;
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
import com.nextcloud.utils.BitmapExtensionsKt;
import com.nextcloud.utils.extensions.OCFileExtensionsKt;
import com.nextcloud.utils.extensions.OwnCloudClientExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.ImageDimension;
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.adapter.DiskLruImageCache;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.nextcloud.utils.extensions.ThumbnailsCacheManagerExtensionsKt.getExifOrientation;

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
    public static final int THUMBNAIL_SIZE_IN_KB = 512;
    private static final int RESIZED_IMAGE_SIZE_IN_KB = 10240;

    public static final Bitmap mDefaultImg = BitmapFactory.decodeResource(MainApp.getAppContext().getResources(),
            R.drawable.file_image);

    public static final Bitmap mDefaultVideo = BitmapFactory.decodeResource(MainApp.getAppContext().getResources(),
            R.drawable.file_movie);

    private ThumbnailsCacheManager() {
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void initDiskCacheAsync() {
        executor.execute(() -> {
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
                        Log_OC.d(TAG, "Disk cache init failed", e);
                        mThumbnailCache = null;
                    }
                }
                mThumbnailCacheStarting = false; // Finished initialization
                mThumbnailsDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
        });
    }

    /**
     * Converts size of file icon from dp to pixel
     * @return int
     */
    public static int getThumbnailDimension() {
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
        int orientation = getExifOrientation(path);
        thumbnail = BitmapExtensionsKt.rotateBitmapViaExif(thumbnail, orientation);

        // Add thumbnail to cache
        // do not overwrite any pre-existing image
        if (!mThumbnailCache.containsKey(imageKey)) {
            addBitmapToCache(imageKey, thumbnail);
        }

        return thumbnail;
    }

    public static void removeFromCache(@Nullable OCFile file) {
        if (file == null) {
            return;
        }

        final var keys = new String[] { PREFIX_RESIZED_IMAGE + file.getRemoteId(), PREFIX_THUMBNAIL + file.getRemoteId() };

        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache == null) {
                return;
            }

            for (String key: keys) {
                mThumbnailCache.removeKey(key);
            }
        }
    }

    public static void addBitmapToCache(String key, Bitmap bitmap) {
        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache == null) {
                return;
            }

            // Check if the bitmap is already cached
            Bitmap cachedBitmap = mThumbnailCache.getBitmap(key);
            if (cachedBitmap == null) {
                cachedBitmap = mThumbnailCache.getScaledBitmap(key, bitmap.getWidth(), bitmap.getHeight());
            }

            if (cachedBitmap != null && BitmapExtensionsKt.allocationKilobyte(cachedBitmap) <= THUMBNAIL_SIZE_IN_KB) {
                Log_OC.d(TAG, "Cached version is already within size limits, no need to scale: " + key);
                return;
            }

            // do not scale down resized images
            int size;
            if (key.startsWith("r")) {
                size = RESIZED_IMAGE_SIZE_IN_KB;
            } else {
                size = THUMBNAIL_SIZE_IN_KB;
            }

            if (BitmapExtensionsKt.allocationKilobyte(bitmap) > size) {
                Log_OC.d(TAG, "Scaling bitmap before caching: " + key);
                bitmap = BitmapExtensionsKt.scaleUntil(bitmap, size);
            }

            mThumbnailCache.put(key, bitmap);
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

    public static Bitmap getScaledThumbnailAfterSave(Bitmap thumbnail, String imageKey) {
        Bitmap result = BitmapExtensionsKt.scaleUntil(thumbnail, THUMBNAIL_SIZE_IN_KB);

        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache != null) {
                Log_OC.d(TAG, "Scaling bitmap before caching: " + imageKey);
                mThumbnailCache.put(imageKey, result);
            }
        }

        return result;
    }

    public static class ResizedImageGenerationTask extends AsyncTask<Object, Void, Bitmap> {
        private final FileFragment fileFragment;
        private final FileDataStorageManager storageManager;
        private final User user;
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<FrameLayout> frameLayoutReference;
        private OCFile file;
        private final ConnectivityService connectivityService;
        private final int backgroundColor;


        public ResizedImageGenerationTask(FileFragment fileFragment,
                                          ImageView imageView,
                                          FrameLayout emptyListProgress,
                                          FileDataStorageManager storageManager,
                                          ConnectivityService connectivityService,
                                          User user,
                                          int backgroundColor) throws IllegalArgumentException {
            this.fileFragment = fileFragment;
            imageViewReference = new WeakReference<>(imageView);
            frameLayoutReference = new WeakReference<>(emptyListProgress);
            this.storageManager = storageManager;
            this.connectivityService = connectivityService;
            this.user = user;
            this.backgroundColor = backgroundColor;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap thumbnail = null;

            file = (OCFile) params[0];

            try {
                mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(user.toOwnCloudAccount(),
                                                                                          MainApp.getAppContext());

                thumbnail = doResizedImageInBackground(file, storageManager);

                if (MimeTypeUtil.isVideo(file) && thumbnail != null) {
                    thumbnail = addVideoOverlay(thumbnail, MainApp.getAppContext());
                }

            } catch (OutOfMemoryError oome) {
                Log_OC.e(TAG, "Out of memory");
            } catch (Throwable t) {
                // the app should never break due to a problem with thumbnails
                Log_OC.e(TAG, "Generation of thumbnail for " + file + " failed", t);
            }

            return thumbnail;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference.get() != null) {
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
                                ((PreviewImageFragment) fileFragment).handleUnsupportedImage();
                            }
                        }
                    }).start();

                }
            }
        }
    }

    public static class ThumbnailGenerationTaskObject {
        private final Object file;
        private final String imageKey;

        public ThumbnailGenerationTaskObject(Object file, @Nullable String imageKey) {
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
        private User user;
        private List<ThumbnailGenerationTask> mAsyncTasks;
        private Object mFile;
        private String mImageKey;
        private FileDataStorageManager mStorageManager;
        private GetMethod getMethod;
        private Listener mListener;
        private boolean gridViewEnabled = false;

        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager, User user)
                throws IllegalArgumentException {
            this(imageView, storageManager, user, null);
        }

        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager,
                                       User user, List<ThumbnailGenerationTask> asyncTasks)
                throws IllegalArgumentException {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
            if (storageManager == null) {
                throw new IllegalArgumentException("storageManager must not be NULL");
            }
            mStorageManager = storageManager;
            this.user = user;
            mAsyncTasks = asyncTasks;
        }

        public ThumbnailGenerationTask(ImageView imageView,
                                       FileDataStorageManager storageManager,
                                       User user,
                                       List<ThumbnailGenerationTask> asyncTasks,
                                       boolean gridViewEnabled,
                                       String imageKey)
            throws IllegalArgumentException {
            this(imageView, storageManager, user, asyncTasks);
            this.gridViewEnabled = gridViewEnabled;
            mImageKey = imageKey;
        }

        public GetMethod getGetMethod() {
            return getMethod;
        }

        public String getImageKey() {
            return mImageKey;
        }

        public ThumbnailGenerationTask(FileDataStorageManager storageManager, User user) {
            if (storageManager == null) {
                throw new IllegalArgumentException("storageManager must not be NULL");
            }
            mStorageManager = storageManager;
            this.user = user;
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
                if (user != null) {
                    OwnCloudAccount ocAccount = user.toOwnCloudAccount();
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                }

                ThumbnailGenerationTaskObject object = params[0];
                mFile = object.getFile();
                mImageKey = object.getImageKey();

                if (mFile instanceof ServerFileInterface) {
                    thumbnail = doThumbnailFromOCFileInBackground();

                    if (MimeTypeUtil.isVideo((ServerFileInterface) mFile) && thumbnail != null) {
                        thumbnail = addVideoOverlay(thumbnail, MainApp.getAppContext());
                    }
                } else if (mFile instanceof File) {
                    thumbnail = doFileInBackground();

                    String url = ((File) mFile).getAbsolutePath();
                    String mMimeType = FileStorageUtils.getMimeTypeFromName(url);

                    if (MimeTypeUtil.isVideo(mMimeType) && thumbnail != null) {
                        thumbnail = addVideoOverlay(thumbnail, MainApp.getAppContext());
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

            boolean updateEnforced = (file instanceof OCFile && ((OCFile) file).isUpdateThumbnailNeeded());

            // Try to load thumbnail from disk cache
            if (!updateEnforced) {
                thumbnail = getBitmapFromDiskCache(imageKey);
                if (thumbnail != null) {
                    Log_OC.d(TAG, "Thumbnail found in disk cache for file: " + file.getFileName());
                    return thumbnail;
                } else {
                    Log_OC.d(TAG, "Thumbnail not found in cache for file: " + file.getFileName());
                }
            } else {
                Log_OC.d(TAG, "Thumbnail update enforced for file: " + file.getFileName());
                thumbnail = null;
            }

            int pxW;
            int pxH;
            pxW = pxH = getThumbnailDimension();

            // Generate thumbnail from local file if available
            if (file instanceof OCFile ocFile && ocFile.isDown()) {
                Log_OC.d(TAG, "Generating thumbnail from local file: " + ocFile.getFileName());

                Bitmap bitmap;
                if (MimeTypeUtil.isVideo(ocFile)) {
                    bitmap = ThumbnailUtils.createVideoThumbnail(ocFile.getStoragePath(),
                                                                 MediaStore.Images.Thumbnails.MINI_KIND);
                } else {
                    bitmap = BitmapUtils.decodeSampledBitmapFromFile(ocFile.getStoragePath(), pxW, pxH);
                }

                if (bitmap != null) {
                    if (PNG_MIMETYPE.equalsIgnoreCase(ocFile.getMimeType())) {
                        bitmap = handlePNG(bitmap, pxW, pxH);
                    }

                    thumbnail = addThumbnailToCache(imageKey, bitmap, ocFile.getStoragePath(), pxW, pxH);
                    ocFile.setUpdateThumbnailNeeded(false);
                    mStorageManager.saveFile(ocFile);
                }
            }

            // Check resized version in disk cache if still null
            if (thumbnail == null) {
                String resizedImageKey = PREFIX_RESIZED_IMAGE + file.getRemoteId();
                Bitmap resizedImage = null;

                if (!updateEnforced) {
                    resizedImage = getBitmapFromDiskCache(resizedImageKey);
                }

                if (resizedImage != null) {
                    thumbnail = ThumbnailUtils.extractThumbnail(resizedImage, pxW, pxH);
                    Log_OC.d(TAG, "Thumbnail generated from resized image cache for file: " + file.getFileName());
                } else {
                    Log_OC.d(TAG, "No resized image cache available for file: " + file.getFileName());
                }
            }

            // Download thumbnail from server if still null
            if (thumbnail == null && mClient != null) {
                Log_OC.d(TAG, "Attempting to download thumbnail from server for file: " + file.getFileName());
                GetMethod getMethod = null;

                try {
                    String uri;
                    if (file instanceof OCFile) {
                        uri = mClient.getBaseUri() + "/index.php/core/preview?fileId="
                            + file.getLocalId()
                            + "&x=" + pxW + "&y=" + pxH + "&a=1&mode=cover&forceIcon=0";
                    } else {
                        uri = mClient.getBaseUri() + "/index.php/apps/files_trashbin/preview?fileId="
                            + file.getLocalId() + "&x=" + pxW + "&y=" + pxH;
                    }

                    Log_OC.d(TAG, "Downloading thumbnail URI: " + uri);

                    getMethod = new GetMethod(uri);
                    getMethod.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");
                    getMethod.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

                    int status = mClient.executeMethod(getMethod, READ_TIMEOUT, CONNECTION_TIMEOUT);

                    if (status == HttpStatus.SC_OK) {
                        try (InputStream inputStream = getMethod.getResponseBodyAsStream()) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (bitmap != null) {
                                thumbnail = ThumbnailUtils.extractThumbnail(bitmap, pxW, pxH);
                                Log_OC.d(TAG, "Thumbnail downloaded and extracted for file: " + file.getFileName());
                            } else {
                                Log_OC.w(TAG, "Downloaded thumbnail bitmap is null for file: " + file.getFileName());
                            }
                        }
                    } else {
                        mClient.exhaustResponse(getMethod.getResponseBodyAsStream());
                        Log_OC.w(TAG, "Failed to download thumbnail, HTTP status: " + status);
                    }

                    if (thumbnail != null && PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                        thumbnail = handlePNG(thumbnail, pxW, pxH);
                        Log_OC.d(TAG, "Handled PNG thumbnail for downloaded file: " + file.getFileName());
                    }
                } catch (Exception e) {
                    Log_OC.e(TAG, "Exception downloading thumbnail for file: " + file.getFileName(), e);
                } finally {
                    if (getMethod != null) {
                        getMethod.releaseConnection();
                    }
                }
            }

            // Add to disk cache if obtained
            if (thumbnail != null) {
                Log_OC.d(TAG, "Adding final thumbnail to cache for file: " + file.getFileName());
                addBitmapToCache(imageKey, thumbnail);
            } else {
                Log_OC.w(TAG, "Failed to obtain thumbnail for file: " + file.getFileName());
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
            double d = Math.pow(2, Math.floor(Math.log(r.getDimension(R.dimen.file_icon_size_grid)) / Math.log(2)));
            return (int) d;
        }

        private Bitmap doFileInBackground() {
            File file = (File)mFile;

            final String imageKey = Objects.requireNonNullElseGet(mImageKey, () -> String.valueOf(file.hashCode()));

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
        @SuppressLint("StaticFieldLeak") private final Context mContext;
        private final ViewThemeUtils viewThemeUtils;

        public MediaThumbnailGenerationTask(ImageView imageView,
                                            Context context,
                                            ViewThemeUtils viewThemeUtils) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
            mContext = context;
            this.viewThemeUtils = viewThemeUtils;
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
                            imageView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(mContext, viewThemeUtils));
                        } else {
                            if (MimeTypeUtil.isVideo(mFile)) {
                                imageView.setImageBitmap(ThumbnailsCacheManager.mDefaultVideo);
                            } else {
                                imageView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null,
                                                                                        mFile.getName(),
                                                                                        mContext,
                                                                                        viewThemeUtils));
                            }
                        }
                    }
                }
            }
        }

        private Bitmap doFileInBackground(File file, Type type) {
            final String imageKey = Objects.requireNonNullElseGet(mImageKey, () -> String.valueOf(file.hashCode()));

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) {

                if (Type.IMAGE == type) {
                    int px = getThumbnailDimension();

                    Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.getAbsolutePath(), px, px);

                    if (bitmap != null) {
                        thumbnail = addThumbnailToCache(imageKey, bitmap, file.getPath(), px, px);
                    }
                } else if (Type.VIDEO == type) {
                    try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                        retriever.setDataSource(file.getAbsolutePath());
                        thumbnail = retriever.getFrameAtTime(-1);
                    } catch (Exception ex) {
                        // can't create a bitmap
                        Log_OC.w(TAG, "Failed to create bitmap from video " + file.getAbsolutePath());
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
        private final User user;
        private final String mUserId;
        private final String displayName;
        private final String mServerName;
        @SuppressLint("StaticFieldLeak") private final Context mContext;


        public AvatarGenerationTask(AvatarGenerationListener avatarGenerationListener,
                                    Object callContext,
                                    User user,
                                    Resources resources,
                                    float avatarRadius,
                                    String userId,
                                    String displayName,
                                    String serverName,
                                    Context context) {
            mAvatarGenerationListener = new WeakReference<>(avatarGenerationListener);
            mCallContext = callContext;
            this.user = user;
            mResources = resources;
            mAvatarRadius = avatarRadius;
            mUserId = userId;
            this.displayName = displayName;
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

        private @NonNull
        Drawable doAvatarInBackground() {
            Bitmap avatar;

            String accountName = mUserId + "@" + mServerName;

            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(mContext);

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

                    int px = mResources.getInteger(R.integer.file_avatar_px);
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
                    return TextDrawable.createAvatarByUserId(displayName, mAvatarRadius);
                } catch (Exception e1) {
                    return ResourcesCompat.getDrawable(mResources, R.drawable.ic_user_outline, null);
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
            if (drawable instanceof AsyncThumbnailDrawable asyncDrawable) {
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static ResizedImageGenerationTask getResizedImageGenerationWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncResizedImageDrawable asyncDrawable) {
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static Bitmap addVideoOverlay(Bitmap thumbnail, Context context) {

        Drawable playButtonDrawable = ResourcesCompat.getDrawable(MainApp.getAppContext().getResources(),
                                                                  R.drawable.video_white,
                                                                  null);

        int px = DisplayUtils.convertDpToPixel(24f, context);

        Bitmap playButton = BitmapUtils.drawableToBitmap(playButtonDrawable, px, px);

        Bitmap resizedPlayButton = Bitmap.createScaledBitmap(playButton, px, px, true);

        Bitmap resultBitmap = Bitmap.createBitmap(thumbnail.getWidth(),
                                                  thumbnail.getHeight(),
                                                  Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(resultBitmap);


        c.drawBitmap(thumbnail, 0, 0, null);

        Paint p = new Paint();
        p.setAlpha(230);

        c.drawBitmap(resizedPlayButton, px, px, p);

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
     * adapted from <a href="https://stackoverflow.com/a/8113368">...</a>
     */
    private static Bitmap handlePNG(Bitmap source, int newWidth, int newHeight) {
        Bitmap softwareBitmap = source.copy(Bitmap.Config.ARGB_8888, false);

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
        int color = ContextCompat.getColor(MainApp.getAppContext(),R.color.background_color_png);
        canvas.drawColor(color);
        canvas.drawBitmap(softwareBitmap, null, targetRect, null);

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

    public static void generateThumbnailFromOCFile(OCFile file, User user, Context context) {
        int pxW;
        int pxH;
        pxW = pxH = getThumbnailDimension();
        String imageKey = PREFIX_THUMBNAIL + file.getRemoteId();

        GetMethod getMethod = null;

        try {
            Bitmap thumbnail = null;

            OwnCloudClient client = mClient;
            if (client == null) {
                OwnCloudAccount ocAccount = user.toOwnCloudAccount();
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

    @VisibleForTesting
    public static void clearCache() {
        mThumbnailCache.clearCache();
        mThumbnailCache = null;
    }

    public static void setClient(OwnCloudClient client) {
        mClient = client;
    }

    public static Bitmap doResizedImageInBackground(OCFile file, FileDataStorageManager storageManager) {
        Bitmap thumbnail;
        String imageKey = PREFIX_RESIZED_IMAGE + file.getRemoteId();

        // Check disk cache in background thread
        thumbnail = getBitmapFromDiskCache(imageKey);
        if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
            Log_OC.d(TAG, "Thumbnail found in cache");
            return thumbnail;
        }

        Point p = getScreenDimension();
        int pxW = p.x;
        int pxH = p.y;

        if (file.isDown()) {
            Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.getStoragePath(), pxW, pxH);
            if (bitmap != null) {
                if (OCFileExtensionsKt.isPNG(file)) {
                    bitmap = handlePNG(bitmap, pxW, pxH);
                }
                thumbnail = addThumbnailToCache(imageKey, bitmap, file.getStoragePath(), pxW, pxH);
                file.setUpdateThumbnailNeeded(false);
            }
        } else if (mClient != null) {
            GetMethod getMethod = null;

            try {
                String uri = OwnCloudClientExtensionsKt.getPreviewEndpoint(mClient, file.getLocalId(), pxW, pxH);
                Log_OC.d(TAG, "generating resized image: " + file.getFileName() + " URI: " + uri);

                getMethod = new GetMethod(uri);
                getMethod.getParams().setSoTimeout(READ_TIMEOUT);

                int status = mClient.executeMethod(getMethod);
                if (status == HttpStatus.SC_OK) {
                    try (InputStream inputStream = getMethod.getResponseBodyAsStream()) {
                        thumbnail = BitmapFactory.decodeStream(inputStream);
                        Log_OC.d(TAG, "resized image generated");
                    }
                } else {
                    Log_OC.e(TAG, "cannot generate thumbnail not supported file type, status: " + status);
                    mClient.exhaustResponse(getMethod.getResponseBodyAsStream());
                }

                if (thumbnail != null && PNG_MIMETYPE.equalsIgnoreCase(file.getMimeType())) {
                    thumbnail = handlePNG(thumbnail, thumbnail.getWidth(), thumbnail.getHeight());
                }

                if (thumbnail != null) {
                    addBitmapToCache(imageKey, thumbnail);
                }
            } catch (Exception e) {
                Log_OC.e(TAG, "doResizedBitmap: ", e);
            } finally {
                if (getMethod != null) {
                    getMethod.releaseConnection();
                }
            }
        }

        // resized dimensions and set update thumbnail needed to false to prevent rendering loop
        if (thumbnail != null && file.getImageDimension() == null) {
            file.setImageDimension(new ImageDimension(thumbnail.getWidth(), thumbnail.getHeight()));
            file.setUpdateThumbnailNeeded(false);
            storageManager.saveFile(file);
        }

        return thumbnail;
    }
}
