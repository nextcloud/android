/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.request.target.Target
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt

class SvgBitmapDecoder(private val bitmapPool: BitmapPool) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(source: InputStream, options: Options): Boolean = true

    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        val svg = try {
            SVG.getFromInputStream(source)
        } catch (e: SVGParseException) {
            throw IOException("Cannot load SVG from stream", e)
        }

        val intrinsicWidth = svg.documentWidth.takeIf { it > 0 } ?: svg.documentViewBox?.width()
        val intrinsicHeight = svg.documentHeight.takeIf { it > 0 } ?: svg.documentViewBox?.height()

        if (svg.documentViewBox == null && intrinsicWidth != null && intrinsicHeight != null) {
            svg.setDocumentViewBox(0f, 0f, intrinsicWidth, intrinsicHeight)
        }
        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")

        val (outWidth, outHeight) = outputSize(width, height, intrinsicWidth, intrinsicHeight)
        val bitmap = bitmapPool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(
            Canvas(bitmap),
            RenderOptions.create().viewPort(0f, 0f, outWidth.toFloat(), outHeight.toFloat())
        )
        return BitmapResource.obtain(bitmap, bitmapPool)
    }

    @Suppress("ReturnCount")
    private fun outputSize(width: Int, height: Int, intrinsicWidth: Float?, intrinsicHeight: Float?): Pair<Int, Int> {
        val isOriginalSize = width == Target.SIZE_ORIGINAL || height == Target.SIZE_ORIGINAL

        if (intrinsicWidth == null || intrinsicHeight == null) {
            return if (isOriginalSize) DEFAULT_SIZE to DEFAULT_SIZE else width to height
        }

        if (isOriginalSize) {
            return intrinsicWidth.toPixels() to intrinsicHeight.toPixels()
        }

        val scale = min(width / intrinsicWidth, height / intrinsicHeight)
        return (intrinsicWidth * scale).toPixels() to (intrinsicHeight * scale).toPixels()
    }

    private fun Float.toPixels(): Int = roundToInt().coerceAtLeast(1)

    companion object {
        private const val DEFAULT_SIZE = 512
    }
}
