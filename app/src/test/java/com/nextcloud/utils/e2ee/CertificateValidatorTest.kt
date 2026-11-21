/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.CertificateValidationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CertificateValidatorTest {
    private val validator = CertificateValidator()

    @Test
    fun preservesBooleanValidationApi() {
        assertEquals(
            true,
            validator.validate(CertificateTestFixtures.publicKey(), CertificateTestFixtures.certificate())
        )
        assertEquals(false, validator.validate(CertificateTestFixtures.publicKey(CertificateTestFixtures.otherKeys),
            CertificateTestFixtures.certificate()))
    }

    @Test
    fun acceptsCertificateSignedByServer() {
        assertEquals(
            CertificateValidationResult.VALID,
            validator.validateWithResult(CertificateTestFixtures.publicKey(), CertificateTestFixtures.certificate())
        )
    }

    @Test
    fun acceptsCrLfAndWhitespaceInServerKey() {
        val serverKey = CertificateTestFixtures.publicKey().replace("\n", "\r\n") + " \t"
        assertEquals(
            CertificateValidationResult.VALID,
            validator.validateWithResult(serverKey, CertificateTestFixtures.certificate())
        )
    }

    @Test
    fun reportsExpiredCertificate() {
        val now = System.currentTimeMillis()
        val certificate = CertificateTestFixtures.certificate(
            notBefore = now - CertificateTestFixtures.DAY_MILLIS * 2,
            notAfter = now - CertificateTestFixtures.DAY_MILLIS
        )
        assertEquals(
            CertificateValidationResult.EXPIRED,
            validator.validateWithResult(CertificateTestFixtures.publicKey(), certificate)
        )
    }

    @Test
    fun reportsCertificateThatIsNotYetValid() {
        val now = System.currentTimeMillis()
        val certificate = CertificateTestFixtures.certificate(
            notBefore = now + CertificateTestFixtures.DAY_MILLIS,
            notAfter = now + CertificateTestFixtures.DAY_MILLIS * 2
        )
        assertEquals(
            CertificateValidationResult.NOT_YET_VALID,
            validator.validateWithResult(CertificateTestFixtures.publicKey(), certificate)
        )
    }

    @Test
    fun reportsMalformedServerKey() {
        assertEquals(
            CertificateValidationResult.INVALID_SERVER_PUBLIC_KEY,
            validator.validateWithResult("not a public key", CertificateTestFixtures.certificate())
        )
    }

    @Test
    fun reportsInvalidServerKeyEncoding() {
        assertEquals(
            CertificateValidationResult.INVALID_SERVER_PUBLIC_KEY,
            validator.validateWithResult("AAAA", CertificateTestFixtures.certificate())
        )
    }

    @Test
    fun reportsMalformedCertificate() {
        assertEquals(
            CertificateValidationResult.INVALID_CERTIFICATE,
            validator.validateWithResult(CertificateTestFixtures.publicKey(), "not a certificate")
        )
    }

    @Test
    fun reportsMismatchedServerSigningKey() {
        assertEquals(
            CertificateValidationResult.INVALID_SIGNATURE,
            validator.validateWithResult(
                CertificateTestFixtures.publicKey(CertificateTestFixtures.otherKeys),
                CertificateTestFixtures.certificate()
            )
        )
    }

    @Test
    fun reportsUnsupportedCertificateSignatureAlgorithm() {
        assertEquals(
            CertificateValidationResult.UNSUPPORTED_ALGORITHM,
            validator.validateWithResult(
                CertificateTestFixtures.publicKey(),
                CertificateTestFixtures.certificate(unsupportedAlgorithm = true)
            )
        )
    }
}
