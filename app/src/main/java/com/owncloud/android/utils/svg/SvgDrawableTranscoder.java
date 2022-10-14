/*
 * Nextcloud Android client application
 *
 * Copyright 2014 Google, Inc. All rights reserved.
 * Licenced under the BSD licence
 *
 * Borrowed from:
 * https://github.com/bumptech/glide/blob/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg/
 * SvgDrawableTranscoder.java
 */
package com.owncloud.android.utils.svg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;

/**
 * Convert the {@link SVG}'s internal representation to an Android-compatible one ({@link Picture}).
 */
public class SvgDrawableTranscoder implements ResourceTranscoder<SVG, Drawable> {
    private final Context context;

    public SvgDrawableTranscoder(Context context) {
        this.context = context;
    }

    @Override
    public Resource<Drawable> transcode(Resource<SVG> toTranscode) {
        SVG svg = toTranscode.get();
        Picture picture = svg.renderToPicture();
        PictureDrawable drawable = new PictureDrawable(picture);

        Bitmap returnedBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                                                    drawable.getIntrinsicHeight(),
                                                    Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawPicture(drawable.getPicture());
        Drawable d = new BitmapDrawable(context.getResources(), returnedBitmap);

        return new SimpleResource<>(d);
    }

    @Override
    public String getId() {
        return "";
    }
}
