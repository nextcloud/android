/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Convert the [SVG]'s internal representation to a Bitmap.
 */
@Suppress("ReturnCount")
class SvgOrImageBitmapTranscoder(private val width: Int, private val height: Int) :
    ResourceTranscoder<SVGorImage, Bitmap> {
    override fun transcode(toTranscode: Resource<SVGorImage>, options: Options): Resource<Bitmap>? {
        val svGorImage = toTranscode.get()
        if (svGorImage.sVG == null) {
            val bitmap = svGorImage.bitmap ?: return null
            return SimpleResource<Bitmap>(bitmap)
        }

        val svg: SVG? = svGorImage.sVG

        try {
            svg?.setDocumentHeight("100%")
            svg?.setDocumentWidth("100%")
        } catch (e: SVGParseException) {
            Log_OC.e(this, "Could not set document size. Output might have wrong size: $e")
        }

        // Create a canvas to draw onto
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Render our document onto our canvas
        svg!!.renderToCanvas(canvas)

        return SimpleResource<Bitmap>(bitmap)
    }
}
