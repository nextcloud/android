/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.content.Context
import android.graphics.Bitmap
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.utils.MimeTypeUtil

class E2eeThumbnailProvider {
    private val user: User
    private val storageManager: FileDataStorageManager
    private val session: E2eeVaultSession
    private val mediaFileLoader: E2eePlaintextMediaLoader
    private val decoder = E2eeThumbnailDecoder()

    @JvmOverloads
    constructor(
        context: Context,
        user: User,
        storageManager: FileDataStorageManager,
        session: E2eeVaultSession,
        arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
    ) : this(
        user,
        storageManager,
        session,
        E2eeMediaFileLoader(context, user, arbitraryDataProvider)
    )

    internal constructor(
        user: User,
        storageManager: FileDataStorageManager,
        session: E2eeVaultSession,
        mediaFileLoader: E2eePlaintextMediaLoader
    ) {
        this.user = user
        this.storageManager = storageManager
        this.session = session
        this.mediaFileLoader = mediaFileLoader
    }

    fun loadThumbnail(file: OCFile, client: OwnCloudClient, width: Int, height: Int): Bitmap? =
        if (canLoadThumbnail(file)) {
            val parent = findParent(file)
            if (parent != null &&
                session.isUnlocked(E2eeVaultSessionKey(user.accountName, parent.localId)) &&
                !E2eeThumbnailMemoryCache.hasRecentFailure(user.accountName, file)
            ) {
                E2eeThumbnailMemoryCache.get(user.accountName, file)
                    ?: loadAndCacheThumbnail(file, client, parent, width, height)
            } else {
                null
            }
        } else {
            null
        }

    private fun canLoadThumbnail(file: OCFile): Boolean =
        file.isEncrypted && !file.isFolder && (MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file))

    private fun loadAndCacheThumbnail(
        file: OCFile,
        client: OwnCloudClient,
        parent: OCFile,
        width: Int,
        height: Int
    ): Bitmap? {
        val thumbnail = mediaFileLoader.withPlaintext(file, parent, client) { plaintext ->
            decoder.decode(file, plaintext, width, height)
        }

        return if (thumbnail != null) {
            E2eeThumbnailMemoryCache.put(user.accountName, file, thumbnail)
        } else {
            E2eeThumbnailMemoryCache.putFailure(user.accountName, file)
            null
        }
    }

    private fun findParent(file: OCFile): OCFile? = storageManager.getFileByEncryptedRemotePath(file.parentRemotePath)
}
