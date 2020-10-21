/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.components

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.RoundedBitmapDrawable

class AvatarWithStatus(val roundedBitmapDrawable: RoundedBitmapDrawable) : Drawable() {
    private val redPaint: Paint = Paint().apply { setARGB(255, 255, 0, 0) }

    override fun draw(canvas: Canvas) {
        val width = 100
        val height = 100
        val radius: Float = Math.min(width, height).toFloat() / 2f

        // Draw a red circle in the center
        //canvas.drawBitmap(roundedBitmapDrawable.bitmap!!, 0f, 0f, null)
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, redPaint)
    }

    override fun setAlpha(alpha: Int) {
        TODO("Not yet implemented")
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("Not yet implemented")
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
