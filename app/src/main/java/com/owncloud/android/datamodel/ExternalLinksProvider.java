/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud.
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
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Database provider for handling the persistence aspects of {@link com.owncloud.android.lib.common.ExternalLink}s.
 */

public class ExternalLinksProvider {
    static private final String TAG = ExternalLinksProvider.class.getSimpleName();

    private ContentResolver mContentResolver;

    public ExternalLinksProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an external link in database.
     *
     * @param externalLink object to store
     * @return external link id, -1 if the insert process fails.
     */
    public long storeExternalLink(ExternalLink externalLink) {
        Log_OC.v(TAG, "Adding " + externalLink.name);

        ContentValues cv = createContentValuesFromExternalLink(externalLink);

        Uri result = mContentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS, cv);

        if (result != null) {
            return Long.parseLong(result.getPathSegments().get(1));
        } else {
            Log_OC.e(TAG, "Failed to insert item " + externalLink.name + " into external link db.");
            return -1;
        }
    }

    /**
     * Delete all external links from the db
     * @return numbers of rows deleted
     */
    public int deleteAllExternalLinks() {
        return mContentResolver.delete(ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS, " 1 = 1 ", null);
    }

    /**
     * get by type external links.
     *
     * @return external links, empty if none exists
     */
    public List<ExternalLink> getExternalLink(ExternalLinkType type) {
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
                null,
                "type = ?",
                new String[]{type.toString()},
                null
        );

        if (cursor != null) {
            List<ExternalLink> list = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    ExternalLink externalLink = createExternalLinkFromCursor(cursor);
                    if (externalLink == null) {
                        Log_OC.e(TAG, "ExternalLink could not be created from cursor");
                    } else {
                        list.add(externalLink);
                    }
                } while (cursor.moveToNext());

            }
            cursor.close();
            return list;
        } else {
            Log_OC.e(TAG, "DB error restoring externalLinks.");
        }

        return new ArrayList<>();
    }

    /**
     * create ContentValues object based on given externalLink.
     *
     * @param externalLink the external Link
     * @return the corresponding ContentValues object
     */
    @NonNull
    private ContentValues createContentValuesFromExternalLink(ExternalLink externalLink) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL, externalLink.iconUrl);
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE, externalLink.language);
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE, externalLink.type.toString());
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME, externalLink.name);
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL, externalLink.url);
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT, externalLink.redirect);
        return cv;
    }

    /**
     * cursor to externalLink
     *
     * @param cursor db cursor
     * @return externalLink, null if cursor is null
     */
    private ExternalLink createExternalLinkFromCursor(Cursor cursor) {
        ExternalLink externalLink = null;
        if (cursor != null) {
            int id = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta._ID));
            String iconUrl = cursor.getString(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL));
            String language = cursor.getString(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE));
            ExternalLinkType type;
            switch (cursor.getString(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE))) {
                case "link":
                    type = ExternalLinkType.LINK;
                    break;
                case "settings":
                    type = ExternalLinkType.SETTINGS;
                    break;
                case "quota":
                    type = ExternalLinkType.QUOTA;
                    break;
                default:
                    type = ExternalLinkType.UNKNOWN;
                    break;
            }
            String name = cursor.getString(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME));
            String url = cursor.getString(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL));
            boolean redirect = cursor.getInt(
                    cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)) == 1;

            externalLink = new ExternalLink(id, iconUrl, language, type, name, url, redirect);
        }
        return externalLink;
    }
}
