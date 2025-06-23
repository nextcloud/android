/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.svg;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.owncloud.android.lib.common.utils.Log_OC;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Convert the {@link SVG}'s internal representation to a Bitmap.
 */
public class SvgOrImageBitmapTranscoder implements ResourceTranscoder<SVGorImage, Bitmap> {
    private int width;
    private int height;

    public SvgOrImageBitmapTranscoder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Nullable
    @Override
    public Resource<Bitmap> transcode(@NonNull Resource<SVGorImage> toTranscode, @NonNull Options options) {
        SVGorImage svGorImage = toTranscode.get();

        if (svGorImage.getSVG() != null) {
            SVG svg = svGorImage.getSVG();

            try {
                svg.setDocumentHeight("100%");
                svg.setDocumentWidth("100%");
            } catch (SVGParseException e) {
                Log_OC.e(this, "Could not set document size. Output might have wrong size");
            }

            // Create a canvas to draw onto
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Render our document onto our canvas
            svg.renderToCanvas(canvas);

            return new SimpleResource<>(bitmap);
        } else {
            Bitmap bitmap = svGorImage.getBitmap();

            return new SimpleResource<>(bitmap);
        }
    }
}
