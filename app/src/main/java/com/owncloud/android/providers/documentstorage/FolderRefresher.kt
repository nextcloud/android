/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import java.io.FileNotFoundException

internal class FolderRefresher(private val context: Context) {

    @Throws(FileNotFoundException::class)
    fun refresh(
        folder: Document,
        storageManager: FileDataStorageManager,
        user: User,
        client: OwnCloudClient
    ): RemoteOperationResult<*> = RefreshFolderOperation(
        folder.requireFile(),
        System.currentTimeMillis(),
        false,
        false,
        true,
        storageManager,
        user,
        context
    ).execute(client)

    fun reload(folder: Document) {
        Log_OC.d(TAG, "reloadFolder(), id=${folder.documentId}")
        try {
            RefreshFolderOperation(
                folder.requireFile(),
                System.currentTimeMillis(),
                false,
                true,
                true,
                folder.storageManager,
                folder.user,
                MainApp.getAppContext()
            ).execute(folder.requireClient(context))
        } catch (e: FileNotFoundException) {
            Log_OC.e(TAG, "reloadFolder() failed, id=${folder.documentId}", e)
        }
    }

    companion object {
        private val TAG = FolderRefresher::class.java.simpleName
    }
}
