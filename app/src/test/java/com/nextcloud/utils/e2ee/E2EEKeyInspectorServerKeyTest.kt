/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import android.net.Uri
import com.nextcloud.client.core.Clock
import com.nextcloud.common.NextcloudClient
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.users.GetCertificateRemoteOperation
import com.owncloud.android.lib.resources.users.GetServerPublicKeyRemoteOperation
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class E2EEKeyInspectorServerKeyTest {
    @Test
    fun repeatedEntryFetchesCertificateAndVerifiesItWhileSharingServerKeyAcrossInspectors() = runTest {
        withContext(Dispatchers.IO) {
            val uri: Uri = mock { on { toString() } doReturn "https://cloud.example" }
            val client: NextcloudClient = mock { on { baseUri } doReturn uri }
            val repository: ClientRepository = mock { onBlocking { getNextcloudClient() } doReturn client }
            val validator: CertificateValidator = mock()
            whenever(validator.validate(SERVER_KEY, CERTIFICATE)).thenReturn(true)
            val cache = E2EEServerKeyCache(mock<Clock>())
            val certificateResult = RemoteOperationResult<String>(ResultCode.OK).apply { resultData = CERTIFICATE }
            val serverKeyResult = RemoteOperationResult<String>(ResultCode.OK).apply { resultData = SERVER_KEY }

            mockConstruction(GetCertificateRemoteOperation::class.java) { operation, _ ->
                whenever(operation.execute(client)).thenReturn(certificateResult)
            }.use { certificates ->
                mockConstruction(GetServerPublicKeyRemoteOperation::class.java) { operation, _ ->
                    whenever(operation.execute(client)).thenReturn(serverKeyResult)
                }.use { serverKeys ->
                    repeat(ENTRIES) {
                        val inspector = E2EEKeyInspector(mock(), mock(), validator, mock(), mock(), repository, cache)
                        assertEquals(DownloadKeyResult.CompareKeys(true), inspector.compareWithServerKey(CERTIFICATE))
                    }
                    assertEquals(ENTRIES, certificates.constructed().size)
                    assertEquals(1, serverKeys.constructed().size)
                }
            }
            verify(validator, times(ENTRIES)).validate(SERVER_KEY, CERTIFICATE)
        }
    }

    companion object {
        private const val SERVER_KEY = "server-key"
        private const val CERTIFICATE = "certificate"
        private const val ENTRIES = 10
    }
}
