/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import com.owncloud.android.R
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils

object VideoOverlayGenerator {

    private const val PLAY_BUTTON_SIZE_IN_DP = 24f
    private const val PLAY_BUTTON_ALPHA = 230

    private val playButtons = mutableMapOf<Int, Bitmap>()

    @JvmStatic
    fun addOverlay(thumbnail: Bitmap, context: Context): Bitmap {
        val overlay = getPlayButton(context) ?: return thumbnail
        val paint = Paint().apply { alpha = PLAY_BUTTON_ALPHA }

        return createBitmap(thumbnail.width, thumbnail.height).applyCanvas {
            drawBitmap(thumbnail, 0f, 0f, null)
            drawBitmap(
                overlay,
                (thumbnail.width - overlay.width) / 2f,
                (thumbnail.height - overlay.height) / 2f,
                paint
            )
        }
    }

    private fun getPlayButton(context: Context): Bitmap? = synchronized(playButtons) {
        val densityDpi = context.resources.displayMetrics.densityDpi

        playButtons[densityDpi]?.takeIf { !it.isRecycled }
            ?: createPlayButton(context)?.also { playButtons[densityDpi] = it }
    }

    private fun createPlayButton(context: Context): Bitmap? {
        val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.video_white, null)
            ?: return null
        val px = DisplayUtils.convertDpToPixel(PLAY_BUTTON_SIZE_IN_DP, context)
        return BitmapUtils.drawableToBitmap(drawable, px, px)
    }
}
