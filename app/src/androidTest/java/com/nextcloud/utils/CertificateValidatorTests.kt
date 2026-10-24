/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.owncloud.android.datamodel.Credentials
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader
import java.security.KeyPairGenerator

class CertificateValidatorTests {

    private var sut: CertificateValidator? = null

    @Before
    fun setup() {
        sut = CertificateValidator()
    }

    @After
    fun destroy() {
        sut = null
    }

    private fun readCredentials(): Credentials {
        val inputStream =
            InstrumentationRegistry.getInstrumentation().context.assets.open("credentials.json")

        return InputStreamReader(inputStream).use { reader ->
            Gson().fromJson(reader, Credentials::class.java)
        }
    }

    private fun generateUnrelatedPublicKeyPem(): String {
        val publicKey = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(RSA_KEY_SIZE) }
            .generateKeyPair()
            .public
        val base64 = Base64.encodeToString(publicKey.encoded, Base64.DEFAULT)
        return "-----BEGIN PUBLIC KEY-----\n$base64-----END PUBLIC KEY-----\n"
    }

    @Test
    fun testValidateWhenGivenValidServerKeyAndCertificateShouldReturnTrue() {
        val credentials = readCredentials()

        assertTrue(sut?.validate(credentials.publicKey, credentials.certificate) ?: false)
    }

    @Test
    fun testValidateWhenCertificateIsNotSignedByServerKeyShouldReturnFalse() {
        val credentials = readCredentials()

        assertFalse(sut?.validate(generateUnrelatedPublicKeyPem(), credentials.certificate) ?: true)
    }

    @Test
    fun testValidateWhenGivenMalformedInputShouldReturnFalse() {
        val credentials = readCredentials()

        assertFalse(sut?.validate("", credentials.certificate) ?: true)
        assertFalse(sut?.validate(credentials.publicKey, "") ?: true)
        assertFalse(sut?.validate(credentials.publicKey, credentials.publicKey) ?: true)
    }

    companion object {
        private const val RSA_KEY_SIZE = 2048
    }
}
