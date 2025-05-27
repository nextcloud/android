/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
     */
    public void storeExternalLink(ExternalLink externalLink) {
        Log_OC.v(TAG, "Adding " + externalLink.getName());

        ContentValues cv = createContentValuesFromExternalLink(externalLink);

        Uri result = mContentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS, cv);

        if (result == null) {
            Log_OC.e(TAG, "Failed to insert item " + externalLink.getName() + " into external link db.");
        }
    }

    /**
     * Delete all external links from the db
     */
    public void deleteAllExternalLinks() {
        mContentResolver.delete(ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
                                null,
                                null);
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
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL, externalLink.getIconUrl());
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE, externalLink.getLanguage());
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE, externalLink.getType().toString());
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME, externalLink.getName());
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL, externalLink.getUrl());
        cv.put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT, externalLink.getRedirect());
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
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta._ID));
            String iconUrl = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL));
            String language = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE));
            ExternalLinkType type = switch (cursor.getString(cursor.getColumnIndexOrThrow(
                ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE))) {
                case "link" -> ExternalLinkType.LINK;
                case "settings" -> ExternalLinkType.SETTINGS;
                case "quota" -> ExternalLinkType.QUOTA;
                default -> ExternalLinkType.UNKNOWN;
            };
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME));
            String url = cursor.getString(cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL));
            boolean redirect = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)) == 1;

            externalLink = new ExternalLink(id, iconUrl, language, type, name, url, redirect);
        }
        return externalLink;
    }
}
