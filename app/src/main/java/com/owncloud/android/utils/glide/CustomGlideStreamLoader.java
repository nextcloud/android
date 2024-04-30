/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;

import java.io.InputStream;

/**
 * Custom Model for OwnCloudClient
 */
public class CustomGlideStreamLoader implements StreamModelLoader<String> {

    private final User user;
    private final ClientFactory clientFactory;

    public CustomGlideStreamLoader(User user, ClientFactory clientFactory) {
        this.user = user;
        this.clientFactory = clientFactory;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(String url, int width, int height) {
        return new HttpStreamFetcher(user, clientFactory, url);
    }
}
