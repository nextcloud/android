/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.e2ee.DeleteEncryptedFilesRemoteOperation
import com.owncloud.android.lib.resources.users.DeletePrivateKeyRemoteOperation
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetPrivateKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetPublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.StorePrivateKeyRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.crypto.CryptoHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteE2ERemoteOperationIT : AbstractOnServerIT() {

    @Test
    fun testDeleteEncryptedFiles() {
        val sut = DeleteEncryptedFilesRemoteOperation()
        val result = sut.execute(nextcloudClient)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testDeletePrivateKey() {
        val keyPair = EncryptionUtils.generateKeyPair()
        val privateKey = keyPair.private
        val keyPhrase = "moreovertelevisionfactorytendencyindependenceinternationalintellectualimpress" +
            "interestvolunteer"
        val privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey)
        val encryptedPrivateKey = CryptoHelper.encryptPrivateKey(
            privatePemKeyString,
            keyPhrase
        )

        StorePrivateKeyRemoteOperation(encryptedPrivateKey).execute(nextcloudClient)

        val sut = DeletePrivateKeyRemoteOperation()
        val result = sut.execute(nextcloudClient)
        assertTrue(result.isSuccess)

        val getResult = GetPrivateKeyRemoteOperation().execute(nextcloudClient)
        assertFalse(getResult.isSuccess)
    }

    @Test
    fun testDeletePublicKey() {
        val sut = DeletePublicKeyRemoteOperation()
        val result = sut.execute(nextcloudClient)
        assertTrue(result.isSuccess)

        val getResult = GetPublicKeyRemoteOperation().execute(nextcloudClient)
        assertFalse(getResult.isSuccess)
    }
}
