/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.setupEncryption

import android.util.Base64
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.crypto.CryptoStringUtils
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
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
    @Suppress("TooGenericExceptionCaught")
    fun validate(serverPublicKeyString: String, certificate: String): Boolean {
        val contentOfServerKey = CryptoStringUtils.rawPublicKey(serverPublicKeyString)

        return try {
            val decodedPublicKey = Base64.decode(contentOfServerKey, Base64.NO_WRAP)

            val keySpec = X509EncodedKeySpec(decodedPublicKey)
            val keyFactory = KeyFactory.getInstance("RSA")
            val serverPublicKey = keyFactory.generatePublic(keySpec)

            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificateInputStream = ByteArrayInputStream(certificate.toByteArray())
            val x509Certificate = certificateFactory.generateCertificate(certificateInputStream) as X509Certificate

            // Check date of the certificate
            x509Certificate.checkValidity()

            // Verify certificate with serverPublicKey
            x509Certificate.verify(serverPublicKey)
            Log_OC.d(tag, "Client certificate is valid against server public key")
            true
        } catch (e: Exception) {
            Log_OC.d(tag, "Client certificate is not valid against the server public key: $e")
            false
        }
    }
}
