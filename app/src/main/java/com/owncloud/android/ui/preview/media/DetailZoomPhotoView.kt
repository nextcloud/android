/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.media

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView

/** [PhotoView] whose zoom levels follow the image's shape, see [DetailZoomScaleLevels]. */
class DetailZoomPhotoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    PhotoView(context, attrs, defStyle) {

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateScaleLevels()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScaleLevels()
    }

    private fun updateScaleLevels() {
        // also called from the ImageView constructor, before PhotoView's attacher exists; the view has no size
        // yet then, so no levels are computed
        val image = drawable ?: return
        val levels = DetailZoomScaleLevels.of(
            viewWidth = width - paddingLeft - paddingRight,
            viewHeight = height - paddingTop - paddingBottom,
            imageWidth = image.intrinsicWidth,
            imageHeight = image.intrinsicHeight,
            bitmapWidth = (image as? BitmapDrawable)?.bitmap?.width ?: image.intrinsicWidth
        ) ?: return
        setScaleLevels(levels.minimum, levels.medium, levels.maximum)
    }
}
