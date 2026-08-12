/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.utils.MimeTypeUtil

@OptIn(UnstableApi::class)
class E2eeVideoMediaSourceProvider internal constructor(
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val session: E2eeVaultSession,
    private val mediaFileLoader: E2eePlaintextMediaLoader,
    private val isVideoRenderable: (ByteArray) -> Boolean = E2eeVideoCompatibilityLogger::isRenderable
) {
    @JvmOverloads
    constructor(
        context: Context,
        user: User,
        storageManager: FileDataStorageManager,
        session: E2eeVaultSession,
        arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
    ) : this(user, storageManager, session, E2eeMediaFileLoader(context, user, arbitraryDataProvider))

    fun loadMediaSource(file: OCFile, client: OwnCloudClient): E2eeVideoMediaSourceHandle? {
        val parent = if (canLoadVideo(file)) {
            storageManager.getFileByEncryptedRemotePath(file.parentRemotePath)
        } else {
            null
        }

        return if (parent != null && session.isUnlocked(E2eeVaultSessionKey(user.accountName, parent.localId))) {
            mediaFileLoader.withPlaintext(file, parent, client) { plaintext ->
                if (isVideoRenderable(plaintext)) {
                    E2eeVideoMediaSourceHandle(file, plaintext.copyOf())
                } else {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun canLoadVideo(file: OCFile): Boolean = file.isEncrypted && !file.isFolder && MimeTypeUtil.isVideo(file)
}
