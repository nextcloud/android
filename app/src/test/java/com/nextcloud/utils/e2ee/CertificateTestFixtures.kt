/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.Date

internal object CertificateTestFixtures {
    const val DAY_MILLIS = 86_400_000L
    private const val KEY_SIZE = 2048
    private const val PEM_LINE_LENGTH = 64
    private const val UNKNOWN_ALGORITHM_OID = "1.2.3.4.5"

    val serverKeys: KeyPair by lazy { generateKeyPair() }
    val userKeys: KeyPair by lazy { generateKeyPair() }
    val otherKeys: KeyPair by lazy { generateKeyPair() }

    fun certificate(
        keys: KeyPair = userKeys,
        serial: Long = 1,
        notBefore: Long = System.currentTimeMillis() - DAY_MILLIS,
        notAfter: Long = System.currentTimeMillis() + DAY_MILLIS,
        unsupportedAlgorithm: Boolean = false
    ): String {
        val builder = JcaX509v3CertificateBuilder(
            X500Name("CN=Encryption server"),
            BigInteger.valueOf(serial),
            Date(notBefore),
            Date(notAfter),
            X500Name("CN=Encryption user"),
            keys.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(serverKeys.private)
        val contentSigner = if (unsupportedAlgorithm) {
            object : ContentSigner by signer {
                override fun getAlgorithmIdentifier(): AlgorithmIdentifier =
                    AlgorithmIdentifier(ASN1ObjectIdentifier(UNKNOWN_ALGORITHM_OID))
            }
        } else {
            signer
        }
        return pem("CERTIFICATE", builder.build(contentSigner).encoded)
    }

    fun publicKey(keys: KeyPair = serverKeys): String = pem("PUBLIC KEY", keys.public.encoded)

    fun privateKey(keys: KeyPair = userKeys): String = Base64.getEncoder().encodeToString(keys.private.encoded)

    fun pem(label: String, bytes: ByteArray): String {
        val body = Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".toByteArray()).encodeToString(bytes)
        return "-----BEGIN $label-----\n$body\n-----END $label-----\n"
    }

    private fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(KEY_SIZE)
    }.generateKeyPair()
}
