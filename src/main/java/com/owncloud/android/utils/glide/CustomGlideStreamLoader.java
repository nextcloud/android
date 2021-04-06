/**
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Alejandro Bautista
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
