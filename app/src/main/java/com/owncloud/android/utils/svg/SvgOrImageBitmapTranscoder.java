/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * Adapted from https://stackoverflow.com/a/54523482
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils.svg;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.owncloud.android.lib.common.utils.Log_OC;

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

    @Override
    public Resource<Bitmap> transcode(Resource<SVGorImage> toTranscode) {
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

    @Override
    public String getId() {
        return "";
    }
}
