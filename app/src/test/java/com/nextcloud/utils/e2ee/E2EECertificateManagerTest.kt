/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.CertificateValidationResult
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class E2EECertificateManagerTest {
    private val user: User = mock()
    private val provider: ArbitraryDataProvider = mock()
    private val manager = E2EECertificateManager(CertificateValidator(), provider)

    @Before
    fun setUp() {
        whenever(provider.getValue(user, EncryptionUtils.PRIVATE_KEY)).thenReturn(CertificateTestFixtures.privateKey())
    }

    @Test
    fun acceptsUnchangedCertificateWithoutWritingKeys() {
        val certificate = CertificateTestFixtures.certificate()
        assertEquals(DownloadKeyResult.CompareKeys(true), compare(certificate, certificate))
        verifyNoCertificateUpdate()
    }

    @Test
    fun refreshesRenewedCertificateWithSamePublicKeyEvenIfStoredCertificateExpired() {
        val now = System.currentTimeMillis()
        val stored = CertificateTestFixtures.certificate(
            notBefore = now - CertificateTestFixtures.DAY_MILLIS * 2,
            notAfter = now - CertificateTestFixtures.DAY_MILLIS
        )
        val renewed = CertificateTestFixtures.certificate(serial = 2)

        assertEquals(DownloadKeyResult.CompareKeys(true), compare(stored, renewed))
        verify(provider).storeOrUpdateKeyValue(user, EncryptionUtils.PUBLIC_KEY, renewed)
        verify(provider, never()).storeOrUpdateKeyValue(eq(user), eq(EncryptionUtils.PRIVATE_KEY), any<String>())
        verify(provider, never()).storeOrUpdateKeyValue(eq(user), eq(EncryptionUtils.MNEMONIC), any<String>())
    }

    @Test
    fun acceptsEquivalentCertificateWithDifferentPemFormatting() {
        val remote = CertificateTestFixtures.certificate()
        val stored = remote.replace("\n", "\r\n") + " \n"

        assertEquals(DownloadKeyResult.CompareKeys(true), compare(stored, remote))
        verify(provider).storeOrUpdateKeyValue(user, EncryptionUtils.PUBLIC_KEY, remote)
    }

    @Test
    fun rejectsActualPublicKeyChangeWithoutOverwritingLocalCertificate() {
        val stored = CertificateTestFixtures.certificate()
        val remote = CertificateTestFixtures.certificate(keys = CertificateTestFixtures.otherKeys)

        assertEquals(DownloadKeyResult.CompareKeys(false), compare(stored, remote))
        verifyNoCertificateUpdate()
    }

    @Test
    fun rejectsExpiredRemoteCertificateEvenWithSameKey() {
        val now = System.currentTimeMillis()
        val expired = CertificateTestFixtures.certificate(
            notBefore = now - CertificateTestFixtures.DAY_MILLIS * 2,
            notAfter = now - CertificateTestFixtures.DAY_MILLIS
        )

        assertEquals(
            DownloadKeyResult.CertificateVerificationFailed(CertificateValidationResult.EXPIRED),
            compare(CertificateTestFixtures.certificate(), expired)
        )
        verifyNoCertificateUpdate()
    }

    @Test
    fun rejectsUntrustedRemoteCertificateBeforeRefreshingLocalState() {
        assertEquals(
            DownloadKeyResult.CertificateVerificationFailed(CertificateValidationResult.INVALID_SIGNATURE),
            manager.compareAndRefresh(
                user,
                CertificateTestFixtures.certificate(),
                CertificateTestFixtures.certificate(serial = 2),
                CertificateTestFixtures.publicKey(CertificateTestFixtures.otherKeys)
            )
        )
        verifyNoCertificateUpdate()
    }

    @Test
    fun rejectsMismatchedLocalPrivateKeyWithoutOverwritingCertificate() {
        whenever(provider.getValue(user, EncryptionUtils.PRIVATE_KEY))
            .thenReturn(CertificateTestFixtures.privateKey(CertificateTestFixtures.otherKeys))

        assertEquals(
            DownloadKeyResult.LocalKeysInvalid(),
            compare(CertificateTestFixtures.certificate(), CertificateTestFixtures.certificate(serial = 2))
        )
        verifyNoCertificateUpdate()
    }

    @Test
    fun reportsMalformedLocalCertificateWithoutChangingKeys() {
        assertEquals(
            DownloadKeyResult.LocalKeysInvalid(),
            compare("invalid local certificate", CertificateTestFixtures.certificate())
        )
        verifyNoCertificateUpdate()
    }

    @Test
    fun reportsMissingPrivateKeyWithoutChangingCertificate() {
        whenever(provider.getValue(user, EncryptionUtils.PRIVATE_KEY)).thenReturn("")

        assertEquals(
            DownloadKeyResult.LocalKeysInvalid(),
            compare(CertificateTestFixtures.certificate(), CertificateTestFixtures.certificate(serial = 2))
        )
        verifyNoCertificateUpdate()
    }

    @Test
    fun acceptsPrivateKeyWithPemHeadersAndCrLf() {
        val pem = CertificateTestFixtures.pem("PRIVATE KEY", CertificateTestFixtures.userKeys.private.encoded)
        whenever(provider.getValue(user, EncryptionUtils.PRIVATE_KEY)).thenReturn(pem.replace("\n", "\r\n"))
        val certificate = CertificateTestFixtures.certificate()

        assertEquals(DownloadKeyResult.CompareKeys(true), compare(certificate, certificate))
    }

    private fun compare(stored: String, remote: String): DownloadKeyResult = manager.compareAndRefresh(
        user,
        stored,
        remote,
        CertificateTestFixtures.publicKey()
    )

    private fun verifyNoCertificateUpdate() {
        verify(provider, never()).storeOrUpdateKeyValue(eq(user), eq(EncryptionUtils.PUBLIC_KEY), any<String>())
    }
}
