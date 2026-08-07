/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.AsyncTask
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
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
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject
import kotlin.math.roundToInt

class FileThumbnailGenerator @Inject constructor(
    private val storageManager: FileDataStorageManager,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val context: Context,
    private val accountManager: UserAccountManager
) : Injectable {

    companion object {
        private const val TAG = "FileThumbnailGenerator"
    }

    private val tasks: MutableList<ThumbnailGenerationTask> = ArrayList()

    fun execute(
        file: OCFile,
        thumbnailView: ImageView,
        gridView: Boolean,
        shimmerThumbnail: LoaderImageView?
    ) {
        if (file.remoteId == null || !file.isPreviewAvailable) {
            setThumbnailFirstTimeForFile(
                file,
                thumbnailView,
                gridView,
                shimmerThumbnail,
            )
            return
        }

        setThumbnailFromCache(
            file,
            thumbnailView,
            gridView,
            shimmerThumbnail
        )
    }

    private fun setThumbnailFirstTimeForFile(
        file: OCFile,
        thumbnailView: ImageView,
        gridView: Boolean,
        shimmerThumbnail: LoaderImageView?
    ) {
        if (file.remoteId != null) {
            generateNewThumbnailIfNecessary(
                file,
                thumbnailView,
                gridView,
                shimmerThumbnail,
            )
            return
        }

        DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)
        val icon = MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, context, viewThemeUtils)
        thumbnailView.setImageDrawable(icon)
    }

    fun setThumbnailFromCache(
        file: OCFile,
        thumbnailView: ImageView,
        gridView: Boolean,
        shimmerThumbnail: LoaderImageView?
    ) {
        val thumbnail = file.smallThumbnail
        if (thumbnail == null || file.isUpdateThumbnailNeeded) {
            generateNewThumbnailIfNecessary(
                file,
                thumbnailView,
                gridView,
                shimmerThumbnail,
            )
            setThumbnailBackgroundForPNGFileIfNeeded(file, context, thumbnailView)
            return
        }

        DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)

        if (MimeTypeUtil.isVideo(file)) {
            val withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail, context)
            thumbnailView.setImageBitmap(withOverlay)
        } else {
            BitmapUtils.setRoundedBitmapAccordingToListType(gridView, thumbnail, thumbnailView)
        }

        setThumbnailBackgroundForPNGFileIfNeeded(file, context, thumbnailView)
    }

    private fun generateNewThumbnailIfNecessary(
        file: OCFile,
        thumbnailView: ImageView,
        gridView: Boolean,
        shimmerThumbnail: LoaderImageView?
    ) {
        if (!ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
            return
        }

        var thumbnail = file.smallThumbnail

        if (thumbnail != null) {
            // If thumbnail is already in cache, display it immediately
            thumbnailView.setImageBitmap(thumbnail)
            DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)
            return
        }

        for (task in tasks) {
            if (file.remoteId != null && task.imageKey != null &&
                file.remoteId == task.imageKey
            ) {
                return
            }
        }

        thumbnailView.tag = file.fileId

        try {
            val task =
                ThumbnailGenerationTask(
                    thumbnailView,
                    storageManager,
                    accountManager.user,
                    tasks,
                    gridView,
                    file.remoteId
                )
            var drawable = MimeTypeUtil.getFileTypeIcon(
                file.mimeType,
                file.fileName,
                context,
                viewThemeUtils
            )
            if (drawable == null) {
                drawable = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.file_image,
                    null
                )
            }
            if (drawable == null) {
                drawable = Color.GRAY.toDrawable()
            }

            val px = ThumbnailsCacheManager.getThumbnailDimension()
            thumbnail = BitmapUtils.drawableToBitmap(drawable, px, px)
            val asyncDrawable =
                AsyncThumbnailDrawable(
                    context.resources,
                    thumbnail, task
                )

            shimmerThumbnail?.postDelayed({
                if (thumbnailView.getDrawable() == null) {
                    if (gridView) {
                        configShimmerGridImageSize(shimmerThumbnail, preferences.getGridColumns())
                    }
                    DisplayUtils.startShimmer(shimmerThumbnail, thumbnailView)
                }
            }, 100)

            task.setListener(object : ThumbnailGenerationTask.Listener {
                override fun onSuccess() {
                    DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)
                }

                override fun onError() {
                    DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)
                    val icon =
                        MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, context, viewThemeUtils)
                    thumbnailView.setImageDrawable(icon)
                    thumbnailView.invalidate()
                    Log_OC.w(TAG, "setting thumbnail failed, using icon from mime type")
                }
            })

            thumbnailView.setImageDrawable(asyncDrawable)
            tasks.add(task)
            task.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                ThumbnailGenerationTaskObject(
                    file,
                    file.remoteId
                )
            )
            thumbnailView.invalidate()
        } catch (e: Exception) {
            Log_OC.d(TAG, "ThumbnailGenerationTask : " + e.message)
        }
    }

    private fun configShimmerGridImageSize(thumbnailShimmer: LoaderImageView, gridColumns: Float) {
        try {
            val targetLayoutParams = thumbnailShimmer.layoutParams as FrameLayout.LayoutParams

            val screenSize = getScreenSize()
            val marginLeftAndRight = targetLayoutParams.leftMargin + targetLayoutParams.rightMargin
            val size = (screenSize.x / gridColumns - marginLeftAndRight).roundToInt()

            val params = FrameLayout.LayoutParams(size, size)
            params.setMargins(
                targetLayoutParams.leftMargin,
                targetLayoutParams.topMargin,
                targetLayoutParams.rightMargin,
                targetLayoutParams.bottomMargin
            )
            thumbnailShimmer.setLayoutParams(params)
        } catch (exception: Exception) {
            Log_OC.e("ConfigShimmer", exception.message)
        }
    }

    private fun getScreenSize(): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        if (windowManager != null) {
            val displaySize = Point()
            windowManager.getDefaultDisplay().getSize(displaySize)
            return displaySize
        } else {
            throw Exception("WindowManager not found")
        }
    }

    private fun setThumbnailBackgroundForPNGFileIfNeeded(
        file: ServerFileInterface,
        context: Context,
        thumbnailView: ImageView
    ) {
        if ("image/png".equals(file.mimeType, ignoreCase = true)) {
            val color = ContextCompat.getColor(context, R.color.bg_default)
            thumbnailView.setBackgroundColor(color)
        }
    }
}
