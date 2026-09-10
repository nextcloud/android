/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.utils.OCFileUtils
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil

fun ImageView.startShimmer(shimmer: LoaderImageView) {
    shimmer.setImageResource(R.drawable.background)
    shimmer.resetLoader()
    setVisibility(View.GONE)
    shimmer.setVisibility(View.VISIBLE)
}

fun ImageView.stopShimmer(shimmer: LoaderImageView?) {
    shimmer?.let {
        it.visibility = View.GONE
    }
    setVisibility(View.VISIBLE)
}

fun ImageView.setMediaPlaceholder(file: OCFile, iconInset: Int) {
    scaleType = ImageView.ScaleType.FIT_CENTER
    setPadding(iconInset, iconInset, iconInset, iconInset)
    setBackgroundResource(R.color.media_placeholder_background)
    foreground = null
    setImageDrawable(OCFileUtils.getMediaPlaceholder(file))
    setTag(R.id.media_thumbnail_file_id, null)
}

fun ImageView.setMediaThumbnail(file: OCFile, bitmap: Bitmap) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    setPadding(0, 0, 0, 0)

    if (file.isPNG()) {
        setBackgroundResource(R.color.bg_default)
    } else {
        background = null
    }

    foreground = if (MimeTypeUtil.isVideo(file)) {
        ContextCompat.getDrawable(context, R.drawable.video_white)
    } else {
        null
    }

    setImageBitmap(bitmap)
    setTag(R.id.media_thumbnail_file_id, file.fileId)
}

fun ImageView.showsMediaThumbnailOf(file: OCFile): Boolean = getTag(R.id.media_thumbnail_file_id) == file.fileId

@JvmOverloads
fun ImageView.makeRoundedWithIcon(
    context: Context,
    @DrawableRes icon: Int,
    paddingDp: Int = 6,
    @ColorInt backgroundColor: Int = ContextCompat.getColor(context, R.color.primary),
    @ColorInt foregroundColor: Int = ContextCompat.getColor(context, R.color.white)
) {
    setImageResource(icon)

    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(backgroundColor)
    }

    background = drawable
    clipToOutline = true
    scaleType = ImageView.ScaleType.CENTER_INSIDE
    outlineProvider = ViewOutlineProvider.BACKGROUND

    setColorFilter(foregroundColor)

    val paddingPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        paddingDp.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
}
