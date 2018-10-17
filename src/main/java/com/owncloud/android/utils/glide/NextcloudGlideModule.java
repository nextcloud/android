/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils.glide;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;
import com.owncloud.android.utils.svg.SvgDecoder;
import com.owncloud.android.utils.svg.SvgDrawableTranscoder;

import java.io.InputStream;

/**
 * Module for generating api.
 */
@GlideModule
public class NextcloudGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(GlideContainer.class, InputStream.class, new GlideContainerModelLoaderFactory());
        registry.prepend(GlideOcFile.class, InputStream.class, new OCFileModelLoaderFactory());
        registry.prepend(String.class, InputStream.class, new StringModelLoaderFactory());
        registry.prepend(GlideAvatar.class, InputStream.class, new AvatarModelLoaderFactory());
        registry.register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
