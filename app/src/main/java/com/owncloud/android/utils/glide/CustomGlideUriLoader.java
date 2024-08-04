/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide;

import android.net.Uri;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;

import java.io.InputStream;

/**
 * Custom Model for authenticated fetching from Uri
 */
public class CustomGlideUriLoader implements StreamModelLoader<Uri> {

    private final User user;
    private final ClientFactory clientFactory;

    public CustomGlideUriLoader(User user, ClientFactory clientFactory) {
        this.user = user;
        this.clientFactory = clientFactory;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(Uri url, int width, int height) {
        return new HttpStreamFetcher(user, clientFactory, url.toString());
    }
}
