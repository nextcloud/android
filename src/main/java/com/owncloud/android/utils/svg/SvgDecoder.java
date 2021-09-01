/*
 * Nextcloud Android client application
 *
 * Copyright 2014 Google, Inc. All rights reserved.
 * Licenced under the BSD licence
 *
 * Borrowed from:
 * https://github.com/bumptech/glide/blob/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg/
 * SvgDecoder.java
 */

package com.owncloud.android.utils.svg;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes an SVG internal representation from an {@link InputStream}.
 */
public class SvgDecoder implements ResourceDecoder<InputStream, SVG> {
    private int height = -1;
    private int width = -1;

    public SvgDecoder(int height, int width) {
        this.height = height;
        this.width = width;
    }

    public SvgDecoder() {
        // empty constructor
    }

    public Resource<SVG> decode(InputStream source, int w, int h) throws IOException {
        try {
            SVG svg = SVG.getFromInputStream(source);

            if (width > 0) {
                svg.setDocumentWidth(width);
            }
            if (height > 0) {
                svg.setDocumentHeight(height);
            }
            svg.setDocumentPreserveAspectRatio(PreserveAspectRatio.LETTERBOX);

            return new SimpleResource<>(svg);
        } catch (SVGParseException ex) {
            throw new IOException("Cannot load SVG from stream", ex);
        }
    }

    @Override
    public String getId() {
        return "SvgDecoder.com.owncloud.android";
    }
}
