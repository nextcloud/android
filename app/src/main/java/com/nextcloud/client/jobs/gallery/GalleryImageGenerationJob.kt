/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.gallery

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.nextcloud.client.account.User
import com.nextcloud.utils.allocationKilobyte
import com.nextcloud.utils.extensions.isPNG
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.WeakHashMap

class GalleryImageGenerationJob(private val user: User, private val storageManager: FileDataStorageManager) {
    companion object {
        private const val TAG = "GalleryImageGenerationJob"
        private val semaphore = Semaphore(
            maxOf(
                3,
                Runtime.getRuntime().availableProcessors() / 2
            )
        )
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

    @Suppress("TooGenericExceptionCaught")
    suspend fun run(file: OCFile, imageView: ImageView, listener: GalleryImageGenerationListener) {
        try {
            var newImage = false

            if (file.remoteId == null && !file.isPreviewAvailable) {
                Log_OC.e(TAG, "file has no remoteId and no preview")
                withContext(Dispatchers.Main) {
                    listener.onError()
                }
                return
            }

            val bitmap: Bitmap? = getBitmap(file, onThumbnailGeneration = {
                newImage = true
            })

            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    listener.onError()
                }
                return
            }

            setThumbnail(bitmap, file, imageView, newImage, listener)
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                listener.onError()
            }
        }
    }

    private suspend fun getBitmap(file: OCFile, onThumbnailGeneration: () -> Unit): Bitmap? =
        withContext(Dispatchers.IO) {
            if (MimeTypeUtil.isVideo(file)) {
                getVideoBitmap(file, onThumbnailGeneration)
            } else {
                getResizedImageBitmap(file, onThumbnailGeneration)
            }
        }

    private fun getVideoBitmap(file: OCFile, onThumbnailGeneration: () -> Unit): Bitmap? {
        val key = ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
        val cached = ThumbnailsCacheManager.getBitmapFromDiskCache(key)

        if (cached != null && !file.isUpdateThumbnailNeeded) {
            return ThumbnailsCacheManager.addVideoOverlay(cached, MainApp.getAppContext())
        }

        onThumbnailGeneration()
        var bitmap: Bitmap? = null
        if (file.isDown) {
            bitmap = createVideoThumbnail(file.storagePath)
        }

        if (bitmap == null) {
            bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(
                ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + file.remoteId
            )
        }

        if (bitmap != null) {
            ThumbnailsCacheManager.addBitmapToCache(key, bitmap)
            return ThumbnailsCacheManager.addVideoOverlay(bitmap, MainApp.getAppContext())
        }
        return null
    }

    private fun createVideoThumbnail(storagePath: String): Bitmap? {
        val file = storagePath.toFile() ?: return null
        val size = ThumbnailsCacheManager.getThumbnailDimension()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ThumbnailUtils.createVideoThumbnail(file, Size(size, size), null)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Failed to create video thumbnail: ${e.message}")
                null
            }
        } else {
            @Suppress("DEPRECATION")
            ThumbnailUtils.createVideoThumbnail(storagePath, MediaStore.Images.Thumbnails.MINI_KIND)
        }
    }

    private suspend fun getResizedImageBitmap(file: OCFile, onThumbnailGeneration: () -> Unit): Bitmap? {
        val key = file.remoteId
        val cachedThumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + key
        )
        if (cachedThumbnail != null && !file.isUpdateThumbnailNeeded) {
            return getThumbnailFromCache(file, cachedThumbnail, key)
        }
        onThumbnailGeneration()
        semaphore.withPermit {
            return getThumbnailFromServerAndAddToCache(file, cachedThumbnail)
        }
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
                    ContextCompat.getColor(
                        MainApp.getAppContext(),
                        R.color.bg_default
                    )
                )
            }

            if (newImage) {
                listener.onNewGalleryImage()
            }

            if (imageView.isAttachedToWindow) {
                imageView.setImageBitmap(bitmap)
                imageView.invalidate()
            }
        }

        listener.onSuccess()
    }

    private fun getThumbnailFromCache(file: OCFile, thumbnail: Bitmap, key: String): Bitmap {
        var result = thumbnail
        if (thumbnail.allocationKilobyte() > ThumbnailsCacheManager.THUMBNAIL_SIZE_IN_KB) {
            result = ThumbnailsCacheManager.getScaledThumbnailAfterSave(result, key)
        }
        if (MimeTypeUtil.isVideo(file)) {
            result = ThumbnailsCacheManager.addVideoOverlay(thumbnail, MainApp.getAppContext())
        }
        return result
    }

    @Suppress("DEPRECATION", "TooGenericExceptionCaught")
    private suspend fun getThumbnailFromServerAndAddToCache(file: OCFile, thumbnail: Bitmap?): Bitmap? {
        var thumbnail = thumbnail
        try {
            val client = withContext(Dispatchers.IO) {
                OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                    user.toOwnCloudAccount(),
                    MainApp.getAppContext()
                )
            }
            ThumbnailsCacheManager.setClient(client)
            thumbnail = ThumbnailsCacheManager.doResizedImageInBackground(file, storageManager)

            if (MimeTypeUtil.isVideo(file) && thumbnail != null) {
                thumbnail = ThumbnailsCacheManager.addVideoOverlay(thumbnail, MainApp.getAppContext())
            }
        } catch (t: Throwable) {
            Log_OC.e(TAG, "Generation of gallery image for $file failed", t)
        }

        return thumbnail
    }
}
