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

import com.caverock.androidsvg.SVG;

public class SVGorImage {
    private SVG svg;
    private Bitmap bitmap;

    public SVGorImage(SVG svg, Bitmap bitmap) {
        this.svg = svg;
        this.bitmap = bitmap;
    }

    public SVG getSVG() {
        return svg;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
