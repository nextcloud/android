/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.e2ee.model.E2EEAction
import com.nextcloud.utils.e2ee.model.E2EEKeyCheck
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.dialog.setupEncryption.model.CertificateValidationResult
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class E2EEActionResolverTest {
    private val user: User = mock()
    private val accountManager: UserAccountManager = mock {
        on { user }.thenReturn(this@E2EEActionResolverTest.user)
    }
    private val provider: ArbitraryDataProvider = mock()
    private val storageManager: FileDataStorageManager = mock()
    private val connectivityService: ConnectivityService = mock()
    private val inspector: E2EEKeyInspector = mock()
    private val resolver = E2EEActionResolver(storageManager, provider, accountManager, connectivityService, inspector)

    @Test
    fun preservesEachCertificateFailureForEveryEncryptionActionWithoutMarkingFolderReadOnly() = runTest {
        val capability: OCCapability = mock {
            on { endToEndEncryptionKeysExist }.thenReturn(CapabilityBooleanType.TRUE)
            on { endToEndEncryption }.thenReturn(CapabilityBooleanType.TRUE)
        }
        whenever(inspector.fetchCapabilities()).thenReturn(null)
        whenever(storageManager.getCapability(user)).thenReturn(capability)
        whenever(provider.getValue(user, EncryptionUtils.PUBLIC_KEY)).thenReturn(STORED_CERTIFICATE)

        CertificateValidationResult.entries.filter { it != CertificateValidationResult.VALID }.forEach { reason ->
            whenever(inspector.compareWithServerKey(STORED_CERTIFICATE))
                .thenReturn(DownloadKeyResult.CertificateVerificationFailed(reason))

            val result = resolver.checkKeys()

            assertEquals(E2EEKeyCheck.CHECK_FAILED, result.state)
            E2EEAction.entries.forEach { action -> assertEquals(reason.descriptionId, result.getMessageId(action)) }
        }
        verify(storageManager, never()).setReadOnly(any(), any())
    }

    @Test
    fun allowsOpeningAfterCertificateRefresh() = runTest {
        val capability: OCCapability = mock {
            on { endToEndEncryptionKeysExist }.thenReturn(CapabilityBooleanType.TRUE)
            on { endToEndEncryption }.thenReturn(CapabilityBooleanType.TRUE)
        }
        whenever(inspector.fetchCapabilities()).thenReturn(null)
        whenever(storageManager.getCapability(user)).thenReturn(capability)
        whenever(provider.getValue(user, EncryptionUtils.PUBLIC_KEY)).thenReturn(STORED_CERTIFICATE)
        whenever(inspector.compareWithServerKey(STORED_CERTIFICATE)).thenReturn(DownloadKeyResult.CompareKeys(true))

        assertEquals(E2EEKeyCheck.SAME_AS_SERVER, resolver.checkKeys().state)
    }

    @Test
    fun preservesRateLimitingBeforeKeyDownloadWithoutTreatingKeysAsMissing() = runTest {
        whenever(inspector.isLocalKeysAbsent()).thenReturn(true)
        whenever(inspector.fetchCapabilities()).thenReturn(
            DownloadKeyResult.RequestFailed(R.string.e2ee_key_request_rate_limited)
        )
        doAnswer {
            it.getArgument<(Boolean) -> Unit>(0)(true)
        }.whenever(connectivityService).isNetworkAndServerAvailable(any())

        val result = resolver.checkKeys()

        E2EEAction.entries.forEach { action ->
            assertEquals(R.string.e2ee_key_request_rate_limited, result.getMessageId(action))
        }
        verify(inspector, never()).compareWithServerKey(any())
        verify(storageManager, never()).setReadOnly(any(), any())
        verify(provider, never()).storeOrUpdateKeyValue(any<User>(), any(), any())
    }

    @Test
    fun preservesHttpFailureReasonsForAllEncryptionActionsWithoutChangingKeys() = runTest {
        val capability: OCCapability = mock {
            on { endToEndEncryptionKeysExist }.thenReturn(CapabilityBooleanType.TRUE)
            on { endToEndEncryption }.thenReturn(CapabilityBooleanType.TRUE)
        }
        whenever(inspector.fetchCapabilities()).thenReturn(null)
        whenever(storageManager.getCapability(user)).thenReturn(capability)
        whenever(provider.getValue(user, EncryptionUtils.PUBLIC_KEY)).thenReturn(STORED_CERTIFICATE)

        for (httpCode in listOf(401, 403, 404, 429, 500, 503, 504)) {
            val response = mock<RemoteOperationResult<String>> {
                on { this.httpCode } doReturn httpCode
            }
            val failure = E2EEKeyRequestError.from(response)
            whenever(inspector.compareWithServerKey(STORED_CERTIFICATE)).thenReturn(failure)

            val result = resolver.checkKeys()

            assertEquals(E2EEKeyCheck.CHECK_FAILED, result.state)
            E2EEAction.entries.forEach { action ->
                assertEquals(failure.descriptionId, result.getMessageId(action))
            }
        }
        verify(storageManager, never()).setReadOnly(any(), any())
        verify(provider, never()).storeOrUpdateKeyValue(any<User>(), any(), any())
    }

    companion object {
        private const val STORED_CERTIFICATE = "stored certificate"
    }
}
