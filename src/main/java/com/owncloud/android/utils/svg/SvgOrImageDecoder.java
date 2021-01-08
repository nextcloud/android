/*
 * Nextcloud Android client application
 *
 * Copyright 2014 Google, Inc. All rights reserved.
 * Licenced under the BSD licence
 *
 * Borrowed from:
 * https://github.com/bumptech/glide/blob/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg/
 * SvgDecoder.java
 *
 * Adapted from https://stackoverflow.com/a/54523482
 *
 */

package com.owncloud.android.utils.svg;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes an SVG internal representation from an {@link InputStream}.
 */
public class SvgOrImageDecoder implements ResourceDecoder<InputStream, SVGorImage> {
    private int height = -1;
    private int width = -1;

    public SvgOrImageDecoder(int height, int width) {
        this.height = height;
        this.width = width;
    }

    public SvgOrImageDecoder() {
        // empty constructor
    }

    public Resource<SVGorImage> decode(InputStream source, int w, int h) throws IOException {
        byte[] array = new byte[source.available()];
        source.read(array);
        ByteArrayInputStream svgInputStream = new ByteArrayInputStream(array.clone());
        ByteArrayInputStream pngInputStream = new ByteArrayInputStream(array.clone());

        try {
            SVG svg = SVG.getFromInputStream(svgInputStream);
            source.close();
            pngInputStream.close();

            if (width > 0) {
                svg.setDocumentWidth(width);
            }
            if (height > 0) {
                svg.setDocumentHeight(height);
            }
            svg.setDocumentPreserveAspectRatio(PreserveAspectRatio.LETTERBOX);

            return new SimpleResource<>(new SVGorImage(svg, null));
        } catch (SVGParseException ex) {
            Bitmap bitmap = BitmapFactory.decodeStream(pngInputStream);
            return new SimpleResource<>(new SVGorImage(null, bitmap));
        }
    }

    @Override
    public String getId() {
        return "SvgDecoder.com.owncloud.android";
    }
}
