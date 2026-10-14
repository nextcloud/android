/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileDetail

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.nextcloud.utils.extensions.getSmallThumbnail
import com.nextcloud.utils.thumbnail.FolderThumbnailGenerator
import com.nextcloud.utils.thumbnail.VideoOverlayGenerator
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

class FileDetailPreview(
    private val toolbarActivity: ToolbarActivity,
    private val folderThumbnailGenerator: FolderThumbnailGenerator,
    private val viewThemeUtils: ViewThemeUtils
) {
    private val previewImageView: ImageView? = toolbarActivity.previewImageView

    fun show(file: OCFile) {
        if (file.isFolder) {
            setIcon(folderThumbnailGenerator.getFolderIcon(file))
            return
        }

        val thumbnail = file.getSmallThumbnail()
        if (thumbnail == null) {
            setIcon(MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, toolbarActivity, viewThemeUtils))
        } else {
            setImage(thumbnail.withVideoOverlay(file))
        }
    }

    private fun Bitmap.withVideoOverlay(file: OCFile): Bitmap = if (MimeTypeUtil.isVideo(file)) {
        VideoOverlayGenerator.addOverlay(this, toolbarActivity)
    } else {
        this
    }

    private fun setImage(bitmap: Bitmap) {
        previewImageView?.setImageBitmap(bitmap)
        applyScaling(ImageView.ScaleType.CENTER_CROP, 0)
    }

    private fun setIcon(icon: Drawable) {
        previewImageView?.setImageDrawable(icon)
        val padding = toolbarActivity.resources.getDimensionPixelSize(R.dimen.standard_double_padding)
        applyScaling(ImageView.ScaleType.FIT_CENTER, padding)
    }

    private fun applyScaling(scaleType: ImageView.ScaleType, padding: Int) {
        previewImageView?.run {
            this.scaleType = scaleType
            setPadding(padding, padding, padding, padding)
        }
        toolbarActivity.setPreviewImageVisibility(true)
    }
}
