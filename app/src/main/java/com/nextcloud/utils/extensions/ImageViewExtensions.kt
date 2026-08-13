/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.R

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
