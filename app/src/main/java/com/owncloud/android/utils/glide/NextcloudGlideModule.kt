/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import com.owncloud.android.utils.svg.SVGorImage
import com.owncloud.android.utils.svg.SvgDecoder
import com.owncloud.android.utils.svg.SvgDrawableTranscoder
import com.owncloud.android.utils.svg.SvgOrImageBitmapTranscoder
import java.io.InputStream

@GlideModule
class NextcloudGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry
            .prepend(Uri::class.java, InputStream::class.java, UriModelLoaderFactory())
            .prepend(String::class.java, InputStream::class.java, StringModelLoaderFactory())
            .register(SVGorImage::class.java, Bitmap::class.java, SvgOrImageBitmapTranscoder(SVG_SIZE, SVG_SIZE))
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    companion object {
        private const val SVG_SIZE = 512
    }
}
