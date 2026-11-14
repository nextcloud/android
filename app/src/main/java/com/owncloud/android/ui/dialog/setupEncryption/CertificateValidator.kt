/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.setupEncryption

import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.setupEncryption.model.CertificateValidationResult
import com.owncloud.android.utils.crypto.CryptoStringUtils
import java.io.ByteArrayInputStream
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateFactory
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.inject.Inject

@Suppress("EmptyClassBlock")
class CertificateValidator @Inject constructor() {
    private val tag = "CertificateValidator"

    /**
     * Validates certificate with given public key
     *
     * @param serverPublicKeyString Public key with header
     * @param certificate Certificate in PEM format
     */
    fun validate(serverPublicKeyString: String, certificate: String): CertificateValidationResult {
        val serverPublicKey = try {
            val decodedPublicKey = Base64.getDecoder().decode(CryptoStringUtils.rawPublicKey(serverPublicKeyString))
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decodedPublicKey))
        } catch (e: IllegalArgumentException) {
            return failure(CertificateValidationResult.INVALID_SERVER_PUBLIC_KEY, e)
        } catch (e: GeneralSecurityException) {
            val result = if (e is NoSuchAlgorithmException) {
                CertificateValidationResult.UNSUPPORTED_ALGORITHM
            } else {
                CertificateValidationResult.INVALID_SERVER_PUBLIC_KEY
            }
            return failure(result, e)
        }

        val x509Certificate = try {
            parseCertificate(certificate)
        } catch (e: CertificateException) {
            return failure(CertificateValidationResult.INVALID_CERTIFICATE, e)
        }

        return try {
            x509Certificate.checkValidity()
            x509Certificate.verify(serverPublicKey)
            Log_OC.d(tag, "Client certificate is valid against server public key")
            CertificateValidationResult.VALID
        } catch (e: GeneralSecurityException) {
            val result = when (e) {
                is CertificateExpiredException -> CertificateValidationResult.EXPIRED
                is CertificateNotYetValidException -> CertificateValidationResult.NOT_YET_VALID
                is SignatureException -> CertificateValidationResult.INVALID_SIGNATURE
                is InvalidKeyException -> CertificateValidationResult.INVALID_SERVER_PUBLIC_KEY
                is NoSuchAlgorithmException -> CertificateValidationResult.UNSUPPORTED_ALGORITHM
                is CertificateException -> CertificateValidationResult.INVALID_CERTIFICATE
                else -> CertificateValidationResult.VERIFICATION_FAILED
            }
            failure(result, e)
        } catch (e: Exception) {
            failure(CertificateValidationResult.VERIFICATION_FAILED, e)
        }
    }

    fun parseCertificate(certificate: String): X509Certificate =
        ByteArrayInputStream(certificate.toByteArray(Charsets.UTF_8)).use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

    private fun failure(result: CertificateValidationResult, exception: Exception): CertificateValidationResult {
        Log_OC.d(tag, "Client certificate verification failed ($result): $exception")
        return result
    }
}
