/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.utils.extensions.getSmallThumbnailKey
import com.nextcloud.utils.extensions.startShimmer
import com.nextcloud.utils.extensions.stopShimmer
import com.nextcloud.utils.extensions.toFile
import com.nextcloud.utils.extensions.videoOverlayKey
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTaskObject
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class FileThumbnailGenerator @Inject constructor(
    private val storageManager: Provider<FileDataStorageManager>,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val context: Context,
    private val accountManager: UserAccountManager
) {

    companion object {
        private const val TAG = "FileThumbnailGenerator"
        private const val SHIMMER_DELAY_MS = 100L
        private const val MIN_THREADS = 3
        private const val CORES_PER_THREAD = 2
        private const val OFFLINE_ICON_SIZE = 105
        private const val OFFLINE_ICON_ALPHA = 100
    }

    private val executor = Executors.newFixedThreadPool(
        maxOf(MIN_THREADS, Runtime.getRuntime().availableProcessors() / CORES_PER_THREAD)
    )

    private val tasks = CopyOnWriteArrayList<ThumbnailGenerationTask>()

    fun setThumbnail(file: OCFile, view: ImageView, arguments: ThumbnailArguments) {
        if (file.remoteId == null) {
            setLocalThumbnail(file, view, arguments)
            return
        }

        if (!file.isPreviewAvailable) {
            generate(file, view, arguments)
            return
        }

        val cached = ThumbnailMemoryCache.get(file.getSmallThumbnailKey())
        if (cached == null || file.isUpdateThumbnailNeeded) {
            generate(file, view, arguments)
        } else {
            show(cached, file, view, arguments)
        }

        applyPngBackground(file, view)
    }

    fun setOfflineOperationThumbnail(file: OCFile, view: ImageView) {
        if (file.isFolder) {
            view.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_folder_offline))
            return
        }

        file.offlineCreateFileBitmap()?.let { bitmap ->
            view.setImageBitmap(BitmapUtils.addColorFilter(bitmap, Color.GRAY, OFFLINE_ICON_ALPHA))
        }
    }

    private fun OCFile.offlineCreateFileBitmap(): Bitmap? {
        val operation = storageManager.get().offlineOperationDao.getByPath(decryptedRemotePath)
        val localPath = (operation?.type as? OfflineOperationType.CreateFile)?.localPath ?: return null

        return BitmapUtils.decodeSampledBitmapFromFile(localPath, OFFLINE_ICON_SIZE, OFFLINE_ICON_SIZE)
    }

    fun cancelPendingTasks() {
        tasks.forEach { task ->
            task.cancel(true)
            task.getMethod?.abort()
        }
        tasks.clear()
    }

    private fun show(bitmap: Bitmap, file: OCFile, view: ImageView, arguments: ThumbnailArguments) {
        view.stopShimmer(arguments.shimmer)

        if (MimeTypeUtil.isVideo(file) && !arguments.hideVideoOverlay) {
            view.setImageBitmap(file.withVideoOverlay(bitmap))
        } else {
            BitmapUtils.setRoundedBitmapAccordingToListType(arguments.isGrid, bitmap, view)
        }
    }

    private fun OCFile.withVideoOverlay(thumbnail: Bitmap): Bitmap {
        val overlayKey = videoOverlayKey(getSmallThumbnailKey())
        ThumbnailMemoryCache.get(overlayKey)?.let { return it }

        return ThumbnailsCacheManager.addVideoOverlay(thumbnail, context).also {
            ThumbnailMemoryCache.put(overlayKey, it)
        }
    }

    private fun setLocalThumbnail(file: OCFile, view: ImageView, arguments: ThumbnailArguments) {
        val localFile = file.storagePath.toFile()

        if (localFile == null || !MimeTypeUtil.isImageOrVideo(file)) {
            view.stopShimmer(arguments.shimmer)
            view.setImageDrawable(file.mimeIcon())
        } else if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(localFile, view)) {
            startTask(
                file,
                view,
                arguments,
                ThumbnailGenerationTaskObject(localFile, null),
                localFile.hashCode()
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun generate(file: OCFile, view: ImageView, arguments: ThumbnailArguments) {
        if (!ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, view)) {
            return
        }

        val cached = ThumbnailMemoryCache.get(file.getSmallThumbnailKey())
        if (cached != null) {
            show(cached, file, view, arguments)
            return
        }

        tasks.removeIf { it.isCancelled || it.status == AsyncTask.Status.FINISHED }

        startTask(
            file,
            view,
            arguments,
            ThumbnailGenerationTaskObject(file, file.remoteId),
            file.fileId
        )
    }

    @Suppress("TooGenericExceptionCaught", "LongParameterList", "DEPRECATION")
    private fun startTask(
        file: OCFile,
        view: ImageView,
        arguments: ThumbnailArguments,
        target: ThumbnailGenerationTaskObject,
        tag: Any
    ) {
        view.tag = tag

        try {
            val task = ThumbnailGenerationTask(
                view,
                storageManager.get(),
                accountManager.user,
                tasks,
                arguments.isGrid,
                file.remoteId,
                arguments.hideVideoOverlay
            ).apply {
                setListener(object : ThumbnailGenerationTask.Listener {
                    override fun onSuccess() = view.stopShimmer(arguments.shimmer)

                    override fun onError() {
                        view.stopShimmer(arguments.shimmer)
                        view.setImageDrawable(file.mimeIcon())
                        view.invalidate()
                        Log_OC.w(TAG, "setting thumbnail failed, using icon from mime type")
                    }
                })
            }
            view.setImageDrawable(AsyncThumbnailDrawable(context.resources, file.placeholder(), task))
            startShimmerLater(view, arguments.isGrid, arguments.shimmer)
            tasks.add(task)
            task.executeOnExecutor(executor, target)
            view.invalidate()
        } catch (e: Exception) {
            Log_OC.d(TAG, "ThumbnailGenerationTask: ${e.message}")
        }
    }

    private fun startShimmerLater(view: ImageView, isGrid: Boolean, shimmer: LoaderImageView?) {
        shimmer?.postDelayed({
            if (view.drawable != null) {
                return@postDelayed
            }

            if (isGrid) {
                shimmer.resizeToGridCell(preferences.gridColumns)
            }

            view.startShimmer(shimmer)
        }, SHIMMER_DELAY_MS)
    }

    private fun LoaderImageView.resizeToGridCell(columns: Float) {
        val current = layoutParams as? FrameLayout.LayoutParams ?: return
        val horizontalMargin = current.leftMargin + current.rightMargin
        val screenWidth = context.resources.displayMetrics.widthPixels
        val size = (screenWidth / columns - horizontalMargin).roundToInt()

        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            setMargins(current.leftMargin, current.topMargin, current.rightMargin, current.bottomMargin)
        }
    }

    private fun OCFile.mimeIcon(): Drawable? = MimeTypeUtil.getFileTypeIcon(mimeType, fileName, context, viewThemeUtils)

    private fun OCFile.placeholder(): Bitmap {
        val drawable = mimeIcon()
            ?: ResourcesCompat.getDrawable(context.resources, R.drawable.file_image, null)
            ?: Color.GRAY.toDrawable()
        val size = ThumbnailsCacheManager.getThumbnailDimension()

        return BitmapUtils.drawableToBitmap(drawable, size, size)
    }

    private fun applyPngBackground(file: ServerFileInterface, view: ImageView) {
        if (!MimeType.PNG.equals(file.mimeType, ignoreCase = true)) {
            return
        }

        view.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_default))
    }
}
