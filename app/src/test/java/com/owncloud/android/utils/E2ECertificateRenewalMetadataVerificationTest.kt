/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.util.Base64
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedUser
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.Mockito
import java.math.BigInteger
import java.security.KeyPair
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

class E2ECertificateRenewalMetadataVerificationTest {

    private val sut = EncryptionUtilsV2()
    private lateinit var base64Mock: MockedStatic<Base64>

    @Before
    fun setUp() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        base64Mock = Mockito.mockStatic(Base64::class.java)
        base64Mock.`when`<String> {
            Base64.encodeToString(ArgumentMatchers.any(ByteArray::class.java), ArgumentMatchers.anyInt())
        }.thenAnswer { invocation ->
            java.util.Base64.getEncoder().encodeToString(invocation.getArgument(0))
        }
        base64Mock.`when`<ByteArray> {
            Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
        }.thenAnswer { invocation ->
            java.util.Base64.getDecoder().decode(invocation.getArgument<String>(0))
        }
    }

    @After
    fun tearDown() {
        base64Mock.close()
    }

    @Test
    fun oldCertificateInMetadataVerifiesSignatureCreatedWithRenewedCertificate() {
        val keyPair = EncryptionUtils.generateKeyPair()
        val oldCertificate = createCertificate(keyPair, OLD_CERTIFICATE_SERIAL)
        val renewedCertificate = createCertificate(keyPair, RENEWED_CERTIFICATE_SERIAL)

        // Renewal keeps the key pair: identical public key, but different certificate bytes.
        assertArrayEquals(oldCertificate.publicKey.encoded, renewedCertificate.publicKey.encoded)
        assertFalse(oldCertificate.encoded.contentEquals(renewedCertificate.encoded))

        val encryptedMetadata = buildEncryptedMetadata(toPem(oldCertificate))
        val message = EncryptionUtils.serializeJSON(encryptedMetadata, true)

        // Metadata is re-signed after renewal, so the signature is produced with the renewed certificate,
        // while the users array still embeds the old certificate carried over from the stored metadata.
        val signature = sut.getMessageSignature(renewedCertificate, keyPair.private, message)
        val decryptedMetadata = buildDecryptedMetadata(toPem(oldCertificate))

        assertTrue(sut.verifyMetadata(encryptedMetadata, decryptedMetadata, INITIAL_COUNTER, signature))
    }

    @Test
    fun unchangedOldMetadataStillVerifiesAfterRenewal() {
        val keyPair = EncryptionUtils.generateKeyPair()
        val oldCertificate = createCertificate(keyPair, OLD_CERTIFICATE_SERIAL)

        val encryptedMetadata = buildEncryptedMetadata(toPem(oldCertificate))
        val message = EncryptionUtils.serializeJSON(encryptedMetadata, true)
        val signature = sut.getMessageSignature(oldCertificate, keyPair.private, message)
        val decryptedMetadata = buildDecryptedMetadata(toPem(oldCertificate))

        assertTrue(sut.verifyMetadata(encryptedMetadata, decryptedMetadata, INITIAL_COUNTER, signature))
    }

    @Test
    fun signatureIsInterchangeableBetweenOldAndRenewedCertificate() {
        val keyPair = EncryptionUtils.generateKeyPair()
        val oldCertificate = createCertificate(keyPair, OLD_CERTIFICATE_SERIAL)
        val renewedCertificate = createCertificate(keyPair, RENEWED_CERTIFICATE_SERIAL)

        val message = EncryptionUtils.serializeJSON(buildEncryptedMetadata(toPem(oldCertificate)), true)
        val signatureWithOld = sut.getMessageSignature(oldCertificate, keyPair.private, message)
        val signatureWithRenewed = sut.getMessageSignature(renewedCertificate, keyPair.private, message)

        assertTrue(sut.verifySignedData(sut.getSignedData(signatureWithOld, message), listOf(renewedCertificate)))
        assertTrue(sut.verifySignedData(sut.getSignedData(signatureWithRenewed, message), listOf(oldCertificate)))
    }

    private fun buildEncryptedMetadata(certificatePem: String) = EncryptedFolderMetadataFile(
        metadata = EncryptedMetadata(
            ciphertext = CIPHERTEXT,
            nonce = NONCE,
            authenticationTag = AUTHENTICATION_TAG
        ),
        users = listOf(EncryptedUser(USER_ID, certificatePem, ENCRYPTED_METADATA_KEY)),
        filedrop = mutableMapOf(),
        version = E2EE_VERSION
    )

    private fun buildDecryptedMetadata(certificatePem: String): DecryptedFolderMetadataFile {
        val metadataKey = EncryptionUtils.generateKey()
        val metadata = DecryptedMetadata(
            keyChecksums = mutableListOf(sut.hashMetadataKey(metadataKey)),
            counter = INITIAL_COUNTER + 1,
            metadataKey = metadataKey
        )

        return DecryptedFolderMetadataFile(
            metadata = metadata,
            users = mutableListOf(DecryptedUser(USER_ID, certificatePem, null)),
            filedrop = mutableMapOf(),
            version = E2EE_VERSION
        )
    }

    private fun createCertificate(keyPair: KeyPair, serial: Long): X509Certificate {
        val name = X500Name("CN=$USER_ID")
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(serial),
            Date(CERTIFICATE_NOT_BEFORE_MILLIS),
            Date(CERTIFICATE_NOT_AFTER_MILLIS),
            name,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

    private fun toPem(certificate: X509Certificate): String {
        val encoded = java.util.Base64.getMimeEncoder(PEM_LINE_LENGTH, LINE_SEPARATOR.toByteArray())
            .encodeToString(certificate.encoded)
        return "$PEM_HEADER\n$encoded\n$PEM_FOOTER\n"
    }

    companion object {
        private const val USER_ID = "alice"
        private const val E2EE_VERSION = "2.0"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val INITIAL_COUNTER = 0L
        private const val OLD_CERTIFICATE_SERIAL = 1L
        private const val RENEWED_CERTIFICATE_SERIAL = 2L
        private const val PEM_LINE_LENGTH = 64
        private const val LINE_SEPARATOR = "\n"
        private const val PEM_HEADER = "-----BEGIN CERTIFICATE-----"
        private const val PEM_FOOTER = "-----END CERTIFICATE-----"
        private const val CERTIFICATE_NOT_BEFORE_MILLIS = 1_700_000_000_000L
        private const val CERTIFICATE_NOT_AFTER_MILLIS = 4_100_000_000_000L
        private const val CIPHERTEXT = "dGVzdC1jaXBoZXJ0ZXh0"
        private const val NONCE = "dGVzdC1ub25jZQ=="
        private const val AUTHENTICATION_TAG = "dGVzdC10YWc="
        private const val ENCRYPTED_METADATA_KEY = "dGVzdC1lbmNyeXB0ZWQta2V5"
    }
}
