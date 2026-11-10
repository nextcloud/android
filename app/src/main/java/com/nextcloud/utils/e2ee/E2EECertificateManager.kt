/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.CertificateValidationResult
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.crypto.CryptoStringUtils
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.inject.Inject

class E2EECertificateManager @Inject constructor(
    private val validator: CertificateValidator,
    private val arbitraryDataProvider: ArbitraryDataProvider
) {
    fun compareAndRefresh(
        user: User,
        storedCertificate: String,
        remoteCertificate: String,
        serverPublicKey: String
    ): DownloadKeyResult {
        val validation = validator.validate(serverPublicKey, remoteCertificate)
        if (validation != CertificateValidationResult.VALID) {
            return DownloadKeyResult.CertificateVerificationFailed(validation)
        }

        val remote = validator.parseCertificate(remoteCertificate)
        try {
            val stored = validator.parseCertificate(storedCertificate)
            if (!stored.publicKey.encoded.contentEquals(remote.publicKey.encoded)) {
                return DownloadKeyResult.CompareKeys(same = false)
            }

            val privateKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY)
            if (!matchesPrivateKey(remote, privateKey)) {
                return DownloadKeyResult.LocalKeysInvalid()
            }
        } catch (e: GeneralSecurityException) {
            Log_OC.w(TAG, "Cannot verify local encryption keys: $e")
            return DownloadKeyResult.LocalKeysInvalid()
        } catch (e: IllegalArgumentException) {
            Log_OC.w(TAG, "Cannot decode local encryption keys: $e")
            return DownloadKeyResult.LocalKeysInvalid()
        }

        if (storedCertificate != remoteCertificate) {
            arbitraryDataProvider.storeOrUpdateKeyValue(user, EncryptionUtils.PUBLIC_KEY, remoteCertificate)
        }
        return DownloadKeyResult.CompareKeys(same = true)
    }

    private fun matchesPrivateKey(certificate: X509Certificate, privateKey: String): Boolean {
        val encodedKey = CryptoStringUtils.rawPrivateKey(privateKey).replace("\\s+".toRegex(), "")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey))
        val key = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        val message = certificate.publicKey.encoded
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(key)
        signer.update(message)
        val signature = signer.sign()

        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(certificate.publicKey)
        verifier.update(message)
        return verifier.verify(signature)
    }

    companion object {
        private const val TAG = "E2EECertificateManager"
    }
}
