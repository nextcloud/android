/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.content.Context
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.DisplayUtils

data class GalleryRow(var files: List<OCFile>, val defaultHeight: Int, val defaultWidth: Int) {
    private fun getMaxHeight(): Float {
        return files.maxOfOrNull { it.imageDimension?.height ?: defaultHeight.toFloat() } ?: 0f
    }

    @SuppressWarnings("MagicNumber", "ComplexMethod")
    fun computeShrinkRatio(context: Context, defaultThumbnailSize: Float, columns: Int): Float {
        val screenWidth =
            DisplayUtils.convertDpToPixel(context.resources.configuration.screenWidthDp.toFloat(), context)
                .toFloat()

        if (files.size > 1) {
            var newSummedWidth = 0f
            for (file in files) {
                // first adjust all thumbnails to max height
                val thumbnail1 = file.imageDimension ?: ImageDimension(defaultThumbnailSize, defaultThumbnailSize)

                val height1 = thumbnail1.height
                val width1 = thumbnail1.width

                val scaleFactor1 = getMaxHeight() / height1
                val newHeight1 = height1 * scaleFactor1
                val newWidth1 = width1 * scaleFactor1

                file.imageDimension = ImageDimension(newWidth1, newHeight1)

                newSummedWidth += newWidth1
            }

            var c = 1f
            // this ensures that files in last row are better visible,
            // e.g. when 2 images are there, it uses 2/5 of screen
            if (columns == 5) {
                when (files.size) {
                    2 -> {
                        c = 5 / 2f
                    }

                    3 -> {
                        c = 4 / 3f
                    }

                    4 -> {
                        c = 4 / 5f
                    }

                    5 -> {
                        c = 1f
                    }
                }
            }

            return (screenWidth / c) / newSummedWidth
        } else {
            val thumbnail1 = files[0].imageDimension ?: ImageDimension(defaultThumbnailSize, defaultThumbnailSize)
            return (screenWidth / columns) / thumbnail1.width
        }
    }
}
