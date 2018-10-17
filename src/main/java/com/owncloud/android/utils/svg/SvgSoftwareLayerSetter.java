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
package com.owncloud.android.utils.svg;

import android.graphics.drawable.PictureDrawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

/**
 * Listener which updates the {@link ImageView} to be software rendered, because
 * {@link com.caverock.androidsvg.SVG SVG}/{@link android.graphics.Picture Picture} can't render on
 * a hardware backed {@link android.graphics.Canvas Canvas}.
 */
public class SvgSoftwareLayerSetter implements RequestListener<PictureDrawable> {
    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<PictureDrawable> target,
                                boolean isFirstResource) {
        try {
            ImageView view = ((ImageViewTarget<?>) target).getView();
            view.setLayerType(ImageView.LAYER_TYPE_NONE, null);
        } catch (Exception e1) {
            // ignore
        }

        return false;
    }

    @Override
    public boolean onResourceReady(PictureDrawable resource, Object model, Target<PictureDrawable> target,
                                   DataSource dataSource, boolean isFirstResource) {
        try {
            ImageView view = ((ImageViewTarget<?>) target).getView();
            view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
        } catch (Exception e) {
            // ignore
        }

        return false;
    }
}
