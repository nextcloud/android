/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.Target
import com.owncloud.android.utils.DisplayUtils.SVG_SIZE
import com.owncloud.android.utils.svg.SvgSoftwareLayerSetter

object GlideHelper {
    private fun createSvgRequestBuilder(
        context: Context,
        uri: Uri,
        @DrawableRes placeholder: Int
    ): RequestBuilder<PictureDrawable>? {
        return Glide
            .with(context)
            .`as`(PictureDrawable::class.java)
            .load(uri)
            .placeholder(placeholder)
            .error(placeholder)
            .listener(SvgSoftwareLayerSetter())
    }

    fun createPictureDrawable(context: Context, icon: String?): PictureDrawable? {
        val uri = icon?.toUri() ?: return null

        return Glide
            .with(context)
            .`as`(PictureDrawable::class.java)
            .load(uri)
            .override(SVG_SIZE, SVG_SIZE)
            .submit()
            .get()
    }

    fun loadSvg(context: Context, icon: String?, imageView: ImageView, @DrawableRes placeholder: Int) {
        val uri = icon?.toUri() ?: return
        val svgRequestBuilder = createSvgRequestBuilder(context, uri, placeholder)
        svgRequestBuilder?.into(imageView)
    }

    fun loadSvg(
        context: Context,
        icon: String?,
        target: Target<PictureDrawable?>,
        placeholder: Int
    ) {
        val uri = icon?.toUri() ?: return
        val svgRequestBuilder = createSvgRequestBuilder(context, uri, placeholder)
        svgRequestBuilder?.into(target)
    }
}
