/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide;

import android.net.Uri;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom Model for authenticated fetching from Uri
 */
public class CustomGlideUriLoader implements ModelLoader<Uri, InputStream> {

    private final User user;
    private final ClientFactory clientFactory;
    
    

    public CustomGlideUriLoader(User user, ClientFactory clientFactory) {
        this.user = user;
        this.clientFactory = clientFactory;
    }


    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull Uri uri, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(uri), new HttpStreamFetcher(user, clientFactory, uri.toString()));
    }

    @Override
    public boolean handles(@NonNull Uri uri) {
        return false;
    }
}
