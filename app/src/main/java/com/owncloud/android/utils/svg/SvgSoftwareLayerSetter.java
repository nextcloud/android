/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2014 Google, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Borrowed from:
 * https://github.com/bumptech/glide/blob/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg/
 * SvgSoftwareLayerSetter.java
 */
package com.owncloud.android.utils.svg;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

public class SvgSoftwareLayerSetter<T> implements RequestListener<T, Drawable> {

    @Override
    public boolean onException(Exception e, T model, Target<Drawable> target, boolean isFirstResource) {
        ImageView view = ((ImageViewTarget<?>) target).getView();
        view.setLayerType(ImageView.LAYER_TYPE_NONE, null);

        return false;
    }

    @Override
    public boolean onResourceReady(Drawable resource, T model, Target<Drawable> target,
                                   boolean isFromMemoryCache, boolean isFirstResource) {
        ImageView view = ((ImageViewTarget<?>) target).getView();
        view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);

        return false;
    }
}
