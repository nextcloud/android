/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

import android.content.ContentValues
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.ShareType

data class ShareeEntry(
    val filePath: String?,
    val accountOwner: String,
    val fileOwnerId: String?,
    val shareWithDisplayName: String?,
    val shareWithUserId: String?,
    val shareType: Int
) {
    companion object {
        /**
         * Extracts a list of share-related ContentValues from a given RemoteFile.
         *
         * Each RemoteFile can be shared with multiple users (sharees), and this function converts each
         * sharee into a ContentValues object, representing a row for insertion into a database.
         *
         * @param remoteFile The RemoteFile object containing sharee information.
         * @param accountName The name of the user account that owns this RemoteFile.
         * @return A list of ContentValues representing each share entry, or null if no sharees are found.
         */
        fun getContentValues(remoteFile: RemoteFile, accountName: String): List<ContentValues>? {
            if (remoteFile.sharees.isNullOrEmpty()) {
                return null
            }

            val result = arrayListOf<ContentValues>()

            for (share in remoteFile.sharees) {
                val shareType: ShareType? = share.shareType
                if (shareType == null) {
                    continue
                }

                val contentValue = ShareeEntry(
                    remoteFile.remotePath,
                    accountName,
                    remoteFile.ownerId,
                    share.displayName,
                    share.userId,
                    shareType.value
                ).toContentValues()

                result.add(contentValue)
            }

            return result
        }
    }

    private fun toContentValues(): ContentValues {
        return ContentValues().apply {
            put(ProviderTableMeta.OCSHARES_PATH, filePath)
            put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, accountOwner)
            put(ProviderTableMeta.OCSHARES_USER_ID, fileOwnerId)
            put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME, shareWithDisplayName)
            put(ProviderTableMeta.OCSHARES_SHARE_WITH, shareWithUserId)
            put(ProviderTableMeta.OCSHARES_SHARE_TYPE, shareType)
        }
    }
}
