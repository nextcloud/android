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

import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

public class GlideAvatar {
    private ObjectKey key;
    private InputStream inputStream;

    public GlideAvatar(ObjectKey key, InputStream avatar) {
        this.key = key;
        this.inputStream = avatar;
    }

    public ObjectKey getKey() {
        return key;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
