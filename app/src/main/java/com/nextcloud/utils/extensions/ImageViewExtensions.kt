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
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.nextcloud.utils.OCFileUtils
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.theme.ViewThemeUtils

fun ImageView.setMediaPlaceholder(file: OCFile, viewThemeUtils: ViewThemeUtils) {
    scaleType = ImageView.ScaleType.FIT_CENTER
    val padding = resources.getDimensionPixelSize(R.dimen.standard_padding)
    setPadding(padding, padding, padding, padding)
    setImageDrawable(OCFileUtils.getMediaPlaceholder(file, viewThemeUtils))
}

fun ImageView.setMediaThumbnail(bitmap: Bitmap) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    setPadding(0, 0, 0, 0)
    setImageBitmap(bitmap)
}

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
