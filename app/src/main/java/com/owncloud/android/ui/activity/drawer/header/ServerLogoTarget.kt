/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.header

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PictureDrawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import kotlin.math.max

class ServerLogoTarget(
    private val resources: Resources,
    private val primaryColor: Int,
    private val onLogoReady: (LayerDrawable) -> Unit
) : CustomTarget<Drawable>() {

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        val bitmap = resource.toLogoBitmap() ?: return
        val layers = arrayOf(primaryColor.toDrawable(), bitmap.scaledToLogoSize().toDrawable(resources))
        onLogoReady(LayerDrawable(layers))
    }

    override fun onLoadCleared(placeholder: Drawable?) = Unit

    private fun Drawable.toLogoBitmap(): Bitmap? = when (this) {
        is PictureDrawable -> createBitmap(intrinsicWidth, intrinsicHeight).also { Canvas(it).drawPicture(picture) }

        is BitmapDrawable -> bitmap

        else -> {
            Log_OC.e(TAG, "Unsupported drawable type: " + javaClass.name)
            null
        }
    }

    private fun Bitmap.scaledToLogoSize(): Bitmap {
        val maxDimension = max(width, height)
        if (maxDimension <= MAX_LOGO_SIZE_PX) return this
        return BitmapUtils.scaleBitmap(this, MAX_LOGO_SIZE_PX.toFloat(), width, height, maxDimension)
    }

    companion object {
        private const val TAG = "ServerLogoTarget"
        private const val MAX_LOGO_SIZE_PX = 1000
    }
}
