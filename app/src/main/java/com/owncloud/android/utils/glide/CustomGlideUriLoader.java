/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
