/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom model for Nextcloud client
 */
public class GlideStringStreamLoader implements ModelLoader<String, InputStream> {
    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull String url, int width, int height, @NonNull Options options) {
        // TODO replace key with etag? and type? (avatar, thumbnail, resized image)
        // TODO pass client to stream fetcher?
        return new LoadData<>(new ObjectKey(url), new GlideStringStreamFetcher(url));
    }

    @Override
    public boolean handles(@NonNull String s) {
        return true;
    }
}
