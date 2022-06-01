/*
 * Nextcloud Android client application
 *
 * Copyright 2014 Google, Inc. All rights reserved.
 * Licenced under the BSD licence
 *
 * Borrowed from:
 * https://github.com/bumptech/glide/blob/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg/
 * SvgSoftwareLayerSetter.java
 */
package com.owncloud.android.utils.svg

import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target

class SvgSoftwareLayerSetter<T> : RequestListener<T, PictureDrawable?> {
    override fun onException(
        e: Exception,
        model: T,
        target: Target<PictureDrawable?>,
        isFirstResource: Boolean
    ): Boolean {
        val view = (target as ImageViewTarget<*>).view
        view.setLayerType(ImageView.LAYER_TYPE_NONE, null)
        return false
    }

    override fun onResourceReady(
        resource: PictureDrawable?,
        model: T,
        target: Target<PictureDrawable?>,
        isFromMemoryCache: Boolean,
        isFirstResource: Boolean
    ): Boolean {
        val view = (target as ImageViewTarget<*>).view
        view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null)
        return false
    }
}
