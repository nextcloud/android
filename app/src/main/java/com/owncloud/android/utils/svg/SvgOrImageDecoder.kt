/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2014 Google, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Borrowed from:
 * https://github.com/bumptech/glide/blob/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg/
 * SvgDecoder.java
 *
 * Adapted from https://stackoverflow.com/a/54523482
 *
 */
package com.owncloud.android.utils.svg

import android.graphics.BitmapFactory
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Decodes an SVG internal representation from an [InputStream].
 */
class SvgOrImageDecoder : ResourceDecoder<InputStream, SVGorImage> {
    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<SVGorImage> {
        val array = ByteArray(source.available())
        source.read(array)
        val svgInputStream = ByteArrayInputStream(array.clone())
        val pngInputStream = ByteArrayInputStream(array.clone())

        try {
            val svg = SVG.getFromInputStream(svgInputStream)
            source.close()
            pngInputStream.close()

            if (width > 0) {
                svg.documentWidth = width.toFloat()
            }
            if (height > 0) {
                svg.documentHeight = height.toFloat()
            }
            svg.documentPreserveAspectRatio = PreserveAspectRatio.LETTERBOX

            return SimpleResource<SVGorImage>(SVGorImage(svg, null))
        } catch (e: SVGParseException) {
            Log_OC.e("SvgOrImageDecoder", e.message)
            val bitmap = BitmapFactory.decodeStream(pngInputStream)
            return SimpleResource<SVGorImage>(SVGorImage(null, bitmap))
        }
    }

    @Throws(IOException::class)
    override fun handles(source: InputStream, options: Options): Boolean {
        return true
    }
}
