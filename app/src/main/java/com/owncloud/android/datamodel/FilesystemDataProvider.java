/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Provider for stored filesystem data.
 */
public class FilesystemDataProvider {

    static private final String TAG = FilesystemDataProvider.class.getSimpleName();

    private final ContentResolver contentResolver;

    public FilesystemDataProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            Log_OC.e(TAG, "couldn't be able constructed, contentResolver is null");
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        this.contentResolver = contentResolver;
    }

    public int deleteAllEntriesForSyncedFolder(String syncedFolderId) {
        Log_OC.d(TAG, "deleteAllEntriesForSyncedFolder called, ID: " + syncedFolderId);

        return contentResolver.delete(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID + " = ?",
            new String[]{syncedFolderId});
    }
}
