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
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.utils.extensions.startShimmer
import com.nextcloud.utils.extensions.stopShimmer
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
import java.util.Collections
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

    private val tasks = Collections.synchronizedList(mutableListOf<ThumbnailGenerationTask>())

    fun setThumbnail(file: OCFile, view: ImageView, isGrid: Boolean, shimmer: LoaderImageView?) {
        if (file.remoteId == null) {
            view.stopShimmer(shimmer)
            view.setImageDrawable(file.mimeIcon())
            return
        }

        if (!file.isPreviewAvailable) {
            generate(file, view, isGrid, shimmer)
            return
        }

        val cached = file.smallThumbnail
        if (cached == null || file.isUpdateThumbnailNeeded) {
            generate(file, view, isGrid, shimmer)
        } else {
            show(cached, file, view, isGrid, shimmer)
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
        synchronized(tasks) {
            tasks.forEach { task ->
                task.cancel(true)
                task.getMethod?.abort()
            }
            tasks.clear()
        }
    }

    private fun show(bitmap: Bitmap, file: OCFile, view: ImageView, isGrid: Boolean, shimmer: LoaderImageView?) {
        view.stopShimmer(shimmer)

        if (MimeTypeUtil.isVideo(file)) {
            view.setImageBitmap(ThumbnailsCacheManager.addVideoOverlay(bitmap, context))
        } else {
            BitmapUtils.setRoundedBitmapAccordingToListType(isGrid, bitmap, view)
        }
    }

    private fun generate(file: OCFile, view: ImageView, isGrid: Boolean, shimmer: LoaderImageView?) {
        if (!ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, view)) {
            return
        }

        file.smallThumbnail?.let { cached ->
            view.setImageBitmap(cached)
            view.stopShimmer(shimmer)
            return
        }

        startTask(file, view, isGrid, shimmer)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startTask(file: OCFile, view: ImageView, isGrid: Boolean, shimmer: LoaderImageView?) {
        if (tasks.any { it.imageKey == file.remoteId }) {
            return
        }

        view.tag = file.fileId

        try {
            val task = newTask(file, view, isGrid, shimmer)
            view.setImageDrawable(AsyncThumbnailDrawable(context.resources, file.placeholder(), task))
            startShimmerLater(view, isGrid, shimmer)
            tasks.add(task)
            task.execute(file)
            view.invalidate()
        } catch (e: Exception) {
            Log_OC.d(TAG, "ThumbnailGenerationTask: ${e.message}")
        }
    }

    private fun newTask(
        file: OCFile,
        view: ImageView,
        isGrid: Boolean,
        shimmer: LoaderImageView?
    ): ThumbnailGenerationTask = ThumbnailGenerationTask(
        view,
        storageManager.get(),
        accountManager.user,
        tasks,
        isGrid,
        file.remoteId
    ).apply {
        setListener(object : ThumbnailGenerationTask.Listener {
            override fun onSuccess() = view.stopShimmer(shimmer)

            override fun onError() {
                view.stopShimmer(shimmer)
                view.setImageDrawable(file.mimeIcon())
                view.invalidate()
                Log_OC.w(TAG, "setting thumbnail failed, using icon from mime type")
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun ThumbnailGenerationTask.execute(file: OCFile) {
        executeOnExecutor(executor, ThumbnailGenerationTaskObject(file, file.remoteId))
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
