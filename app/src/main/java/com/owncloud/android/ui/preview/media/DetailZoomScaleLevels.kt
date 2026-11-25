/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.media

import kotlin.math.max
import kotlin.math.min

/**
 * PhotoView zoom levels, relative to the image fitted inside the view.
 *
 * PhotoView's fixed 1x / 1.75x / 3x leave a very tall image only a few dozen pixels wide even at maximum zoom.
 * Here, double tap fills the view along the image's short side for images whose aspect ratio differs strongly
 * from the view's, and the maximum zoom always reaches beyond both that level and one image pixel per screen pixel.
 */
data class DetailZoomScaleLevels(val minimum: Float, val medium: Float, val maximum: Float) {
    companion object {
        private const val DEFAULT_MINIMUM_SCALE = 1f
        private const val DEFAULT_MEDIUM_SCALE = 1.75f
        private const val DEFAULT_MAXIMUM_SCALE = 3f
        private const val ELONGATED_FILL_SCALE = 2f
        private const val DETAIL_HEADROOM = 3f

        /**
         * [imageWidth] and [imageHeight] are the drawable's intrinsic size; [bitmapWidth] is its width in pixels,
         * which differs when the bitmap is scaled for the screen density. Returns null until all sizes are known.
         */
        fun of(
            viewWidth: Int,
            viewHeight: Int,
            imageWidth: Int,
            imageHeight: Int,
            bitmapWidth: Int
        ): DetailZoomScaleLevels? {
            if (minOf(minOf(viewWidth, viewHeight, imageWidth), minOf(imageHeight, bitmapWidth)) <= 0) {
                return null
            }

            val widthRatio = viewWidth.toFloat() / imageWidth
            val heightRatio = viewHeight.toFloat() / imageHeight
            val fitScale = min(widthRatio, heightRatio)
            val fillScale = max(widthRatio, heightRatio) / fitScale
            val onePixelScale = bitmapWidth / (imageWidth * fitScale)

            val medium = if (fillScale >= ELONGATED_FILL_SCALE) fillScale else DEFAULT_MEDIUM_SCALE
            val maximum = maxOf(DEFAULT_MAXIMUM_SCALE, medium * DETAIL_HEADROOM, onePixelScale * DETAIL_HEADROOM)
            return DetailZoomScaleLevels(DEFAULT_MINIMUM_SCALE, medium, maximum)
        }
    }
}
