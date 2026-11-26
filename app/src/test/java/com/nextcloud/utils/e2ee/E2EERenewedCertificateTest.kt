/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import android.util.Base64
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.utils.EncryptionUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.KeyPair

class E2EERenewedCertificateTest {
    private val user: User = mock()
    private val provider: ArbitraryDataProvider = mock()
    private val inspector = E2EEKeyInspector(mock(), mock(), mock(), provider, mock())
    private val now = System.currentTimeMillis()
    private val day = CertificateTestFixtures.DAY_MILLIS
    private val stored = CertificateTestFixtures.certificate(notAfter = now + day)
    private val renewed = CertificateTestFixtures.certificate(serial = 2, notAfter = now + 2 * day)

    @Test
    fun validRenewalWithMatchingPrivateKeyReplacesStoredCertificate() {
        whenever(provider.getValue(user, EncryptionUtils.CERTIFICATE)).thenReturn(stored)

        withPrivateKey(CertificateTestFixtures.userKeys) {
            assertTrue(inspector.compareVerifiedCertificate(user, stored, renewed))
        }

        verify(provider).storeOrUpdateKeyValue(user, EncryptionUtils.CERTIFICATE, renewed)
    }

    @Test
    fun differentPublicKeyDoesNotReplaceStoredCertificate() {
        val replacement = CertificateTestFixtures.certificate(keys = CertificateTestFixtures.otherKeys)

        assertFalse(inspector.compareVerifiedCertificate(user, stored, replacement))
        verifyNoCertificateWrite()
    }

    @Test
    fun renewalDoesNotOverwriteCertificateChangedDuringRequest() {
        whenever(provider.getValue(user, EncryptionUtils.CERTIFICATE)).thenReturn(renewed)

        withPrivateKey(CertificateTestFixtures.userKeys) {
            assertTrue(inspector.compareVerifiedCertificate(user, stored, renewed))
        }

        verifyNoCertificateWrite()
    }

    @Test
    fun nonMatchingPrivateKeyDoesNotReplaceStoredCertificate() {
        withPrivateKey(CertificateTestFixtures.otherKeys) {
            assertTrue(inspector.compareVerifiedCertificate(user, stored, renewed))
        }

        verifyNoCertificateWrite()
    }

    @Test
    fun olderOrNotYetValidCertificateDoesNotReplaceCurrentCertificate() {
        val invalid = CertificateTestFixtures.certificate(
            serial = 3,
            notBefore = now + day,
            notAfter = now + 2 * day
        )

        assertTrue(inspector.compareVerifiedCertificate(user, renewed, stored))
        assertTrue(inspector.compareVerifiedCertificate(user, stored, invalid))
        verifyNoCertificateWrite()
    }

    private fun withPrivateKey(keys: KeyPair, block: () -> Unit) {
        val encoded = CertificateTestFixtures.privateKey(keys)
        whenever(provider.getValue(user, EncryptionUtils.PRIVATE_KEY)).thenReturn(encoded)
        mockStatic(Base64::class.java).use { base64 ->
            base64.`when`<ByteArray> { Base64.decode(encoded, Base64.NO_WRAP) }.thenReturn(keys.private.encoded)
            block()
        }
    }

    private fun verifyNoCertificateWrite() {
        verify(provider, never()).storeOrUpdateKeyValue(eq(user), eq(EncryptionUtils.CERTIFICATE), any())
    }
}
