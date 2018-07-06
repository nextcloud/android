/*
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * Copyright (C) 2017 Alejandro Bautista
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import static com.owncloud.android.utils.glide.GlideKey.RESIZED_IMAGE_KEY;
import static com.owncloud.android.utils.glide.GlideKey.THUMBNAIL_KEY;

/**
 * Custom model for Nextcloud Client
 */
public class GlideContainerStreamLoader implements ModelLoader<GlideContainer, InputStream> {
    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull GlideContainer container, int width, int height,
                                               @NonNull Options options) {
        return new LoadData<>(container.key, new HttpStreamGlideContainerFetcher(container));
    }

    @Override
    public boolean handles(@NonNull GlideContainer s) {
        return !s.key.equals(new ObjectKey(THUMBNAIL_KEY)) && !s.key.equals(new ObjectKey(RESIZED_IMAGE_KEY));
    }
}
