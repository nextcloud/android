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

import android.accounts.Account;
import android.content.Context;

import com.bumptech.glide.signature.ObjectKey;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.TrashbinFile;

import java.io.File;

public final class GlideKey {
    public static final String AVATAR_KEY = "AVATAR";
    static final String THUMBNAIL_KEY = "THUMBNAIL_";
    static final String RESIZED_IMAGE_KEY = "RESIZED_IMAGE_";

    private GlideKey() {
        // Required empty constructor
    }

    public static ObjectKey serverThumbnail(OCFile file) {
        return new ObjectKey(THUMBNAIL_KEY + file.getEtagOnServer());
    }

    public static ObjectKey resizedImage(OCFile file) {
        return new ObjectKey(RESIZED_IMAGE_KEY + file.getEtagOnServer());
    }

    public static ObjectKey localFile(File file) {
        return new ObjectKey(file.hashCode());
    }

    public static ObjectKey url(String url) {
        return new ObjectKey(url);
    }

    public static ObjectKey trashbinThumbnail(TrashbinFile file) {
        return new ObjectKey(THUMBNAIL_KEY + file.getRemoteId());
    }

    public static ObjectKey activityThumbnail(OCFile file) {
        return new ObjectKey(THUMBNAIL_KEY + file.getRemoteId());
    }

    public static ObjectKey avatar(Account account, String userId, Context context) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());

        String serverName = account.name.substring(account.name.lastIndexOf('@') + 1, account.name.length());
        String eTag = arbitraryDataProvider.getValue(userId + "@" + serverName, GlideKey.AVATAR_KEY);

        return new ObjectKey("a_" + userId + "_" + serverName + "_" + eTag);
    }
}
