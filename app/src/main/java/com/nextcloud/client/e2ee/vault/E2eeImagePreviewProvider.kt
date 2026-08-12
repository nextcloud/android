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

class E2eeImagePreviewProvider internal constructor(
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val session: E2eeVaultSession,
    private val mediaFileLoader: E2eePlaintextMediaLoader,
    private val decoder: E2eeImageDecoder
) {
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
        E2eeMediaFileLoader(context, user, arbitraryDataProvider),
        E2eeImagePreviewDecoder()
    )

    fun loadBitmap(file: OCFile, client: OwnCloudClient, width: Int, height: Int): Bitmap? {
        val parent = if (canLoadPreview(file)) {
            storageManager.getFileByEncryptedRemotePath(file.parentRemotePath)
        } else {
            null
        }

        return if (parent != null && session.isUnlocked(E2eeVaultSessionKey(user.accountName, parent.localId))) {
            mediaFileLoader.withPlaintext(file, parent, client) { plaintext ->
                decoder.decode(plaintext, width, height)
            }
        } else {
            null
        }
    }

    private fun canLoadPreview(file: OCFile): Boolean = file.isEncrypted && !file.isFolder && MimeTypeUtil.isImage(file)
}
