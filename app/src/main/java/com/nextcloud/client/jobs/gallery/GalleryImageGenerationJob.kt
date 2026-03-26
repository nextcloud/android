/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.gallery

import android.graphics.Bitmap
import android.graphics.Point
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.isPNG
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.WeakHashMap

@Suppress("DEPRECATION", "TooGenericExceptionCaught", "ReturnCount")
class GalleryImageGenerationJob(private val user: User, private val storageManager: FileDataStorageManager) {

    companion object {
        private const val TAG = "GalleryImageGenerationJob"
        private val semaphore = Semaphore(maxOf(3, Runtime.getRuntime().availableProcessors() / 2))
        private val activeJobs = Collections.synchronizedMap(WeakHashMap<ImageView, Job>())

        fun cancelAllActiveJobs() {
            val jobsToCancel = synchronized(activeJobs) {
                val list = activeJobs.values.toList()
                activeJobs.clear()
                list
            }
            for (job in jobsToCancel) {
                job.cancel()
            }
        }

        fun removeActiveJob(imageView: ImageView, job: Job) {
            synchronized(activeJobs) {
                if (isActiveJob(imageView, job)) {
                    removeJob(imageView)
                }
            }
        }

        fun isActiveJob(imageView: ImageView, job: Job): Boolean = synchronized(activeJobs) {
            activeJobs[imageView] === job
        }

        fun storeJob(job: Job, imageView: ImageView) {
            synchronized(activeJobs) {
                activeJobs[imageView] = job
            }
        }

        fun cancelPreviousJob(imageView: ImageView) {
            synchronized(activeJobs) {
                activeJobs[imageView]?.cancel()
                activeJobs.remove(imageView)
            }
        }

        fun removeJob(imageView: ImageView) {
            synchronized(activeJobs) {
                activeJobs.remove(imageView)
            }
        }
    }

    suspend fun run(file: OCFile, imageView: ImageView, listener: GalleryImageGenerationListener) {
        try {
            if (file.remoteId == null && !file.isPreviewAvailable) {
                Log_OC.e(TAG, "file has no remoteId and no preview")
                withContext(Dispatchers.Main) { listener.onError() }
                return
            }

            var newImage = false
            val bitmap: Bitmap? = getBitmap(file, onNewThumbnail = { newImage = true })

            if (bitmap == null) {
                withContext(Dispatchers.Main) { listener.onError() }
                return
            }

            setThumbnail(bitmap, file, imageView, newImage, listener)
        } catch (_: Exception) {
            withContext(Dispatchers.Main) { listener.onError() }
        }
    }

    private suspend fun getBitmap(file: OCFile, onNewThumbnail: () -> Unit): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + file.remoteId

        val cached = ThumbnailsCacheManager.getBitmapFromDiskCache(cacheKey)
        if (cached != null && !file.isUpdateThumbnailNeeded) {
            return@withContext applyVideoOverlayIfNeeded(file, cached)
        }

        onNewThumbnail()

        val local = decodeLocalThumbnail(file)
        if (local != null) {
            ThumbnailsCacheManager.addBitmapToCache(cacheKey, local)
            return@withContext applyVideoOverlayIfNeeded(file, local)
        }

        val remote = semaphore.withPermit { fetchFromServer(file) }
        if (remote != null) {
            return@withContext applyVideoOverlayIfNeeded(file, remote)
        }

        null
    }

    private fun decodeLocalThumbnail(file: OCFile): Bitmap? = if (MimeTypeUtil.isVideo(file)) {
        createVideoThumbnail(file.storagePath)
    } else {
        createImageThumbnail(file)
    }

    private fun createImageThumbnail(file: OCFile): Bitmap? {
        val wm = MainApp.getAppContext().getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getSize(p)

        val pxW = p.x
        val pxH = p.y

        val cacheKey = ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + file.remoteId

        var bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.storagePath, pxW, pxH) ?: return null

        if (file.isPNG()) {
            bitmap = ThumbnailsCacheManager.handlePNG(bitmap, pxW, pxH)
        }

        val thumbnail = ThumbnailsCacheManager.addThumbnailToCache(cacheKey, bitmap, file.storagePath, pxW, pxH)
        file.isUpdateThumbnailNeeded = false

        return thumbnail
    }

    private fun createVideoThumbnail(storagePath: String): Bitmap? {
        val ioFile = storagePath.toFile() ?: return null
        val size = ThumbnailsCacheManager.getThumbnailDimension()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ThumbnailUtils.createVideoThumbnail(ioFile, Size(size, size), null)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Failed to create video thumbnail from local file: ${e.message}")
                null
            }
        } else {
            @Suppress("DEPRECATION")
            ThumbnailUtils.createVideoThumbnail(storagePath, MediaStore.Images.Thumbnails.MINI_KIND)
        }
    }

    private suspend fun fetchFromServer(file: OCFile): Bitmap? = try {
        val client = withContext(Dispatchers.IO) {
            OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(user.toOwnCloudAccount(), MainApp.getAppContext())
        }
        ThumbnailsCacheManager.setClient(client)
        ThumbnailsCacheManager.doResizedImageInBackground(file, storageManager)
    } catch (t: Throwable) {
        Log_OC.e(TAG, "Server fetch failed for $file", t)
        null
    }

    private fun applyVideoOverlayIfNeeded(file: OCFile, bitmap: Bitmap): Bitmap = if (MimeTypeUtil.isVideo(file)) {
        ThumbnailsCacheManager.addVideoOverlay(bitmap, MainApp.getAppContext())
    } else {
        bitmap
    }

    private suspend fun setThumbnail(
        bitmap: Bitmap,
        file: OCFile,
        imageView: ImageView,
        newImage: Boolean,
        listener: GalleryImageGenerationListener
    ) = withContext(Dispatchers.Main) {
        val tagId = file.fileId.toString()

        if (imageView.tag.toString() == tagId) {
            if (file.isPNG()) {
                imageView.setBackgroundColor(
                    ContextCompat.getColor(MainApp.getAppContext(), R.color.bg_default)
                )
            }

            if (newImage) listener.onNewGalleryImage()

            if (imageView.isAttachedToWindow) {
                imageView.setImageBitmap(bitmap)
                imageView.invalidate()
            }
        }

        listener.onSuccess()
    }
}
