/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.common.NextcloudClient
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.users.GetCertificateRemoteOperation
import com.owncloud.android.lib.resources.users.GetServerPublicKeyRemoteOperation
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class E2EEKeyInspectorTrustTest {
    @Test
    fun verifiesCertificateWithStoredSigningKeyWithoutDownloadingAnotherKey() = runTest {
        inspect(CertificateTestFixtures.publicKey(), DownloadKeyResult.CompareKeys(true))
    }

    @Test
    fun rejectsCertificateSignedByAnotherKeyWithoutReplacingStoredTrust() = runTest {
        inspect(
            CertificateTestFixtures.publicKey(CertificateTestFixtures.otherKeys),
            DownloadKeyResult.CertificateNotSignedByStoredServerKey
        )
    }

    @Test
    fun storesInitiallyDownloadedSigningKeyAfterSuccessfulVerification() = runTest {
        inspect("", DownloadKeyResult.CompareKeys(true))
    }

    private suspend fun inspect(storedSigningKey: String, expected: DownloadKeyResult) = withContext(Dispatchers.IO) {
        val certificate = CertificateTestFixtures.certificate()
        val serverKey = CertificateTestFixtures.publicKey()
        val user: User = mock()
        val accounts: UserAccountManager = mock { on { this.user } doReturn user }
        val provider: ArbitraryDataProvider = mock {
            on { getValue(user, EncryptionUtils.SERVER_PUBLIC_KEY) } doReturn storedSigningKey
            on { getValue(user, EncryptionUtils.PRIVATE_KEY) } doReturn CertificateTestFixtures.privateKey()
        }
        val client: NextcloudClient = mock()
        val repository: ClientRepository = mock { onBlocking { getNextcloudClient() } doReturn client }
        val certificateResult = RemoteOperationResult<String>(ResultCode.OK).apply { resultData = certificate }
        val signingKeyResult = RemoteOperationResult<String>(ResultCode.OK).apply { resultData = serverKey }
        val manager = E2EECertificateManager(CertificateValidator(), provider)
        val inspector = E2EEKeyInspector(mock(), mock(), manager, provider, accounts, repository)

        mockConstruction(GetCertificateRemoteOperation::class.java) { operation, _ ->
            whenever(operation.execute(client)).thenReturn(certificateResult)
        }.use {
            mockConstruction(GetServerPublicKeyRemoteOperation::class.java) { operation, _ ->
                whenever(operation.execute(client)).thenReturn(signingKeyResult)
            }.use { signingKeys ->
                assertEquals(expected, inspector.compareWithServerCertificate(certificate))
                assertEquals(if (storedSigningKey.isEmpty()) 1 else 0, signingKeys.constructed().size)
            }
        }
        if (storedSigningKey.isEmpty()) {
            verify(provider).storeOrUpdateKeyValue(user, EncryptionUtils.SERVER_PUBLIC_KEY, serverKey)
        } else {
            verify(provider, never()).storeOrUpdateKeyValue(any<User>(), any(), any())
        }
    }
}
