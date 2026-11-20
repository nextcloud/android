/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.owncloud.android.datamodel.Credentials
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

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

    @Test
    fun testValidateWhenGivenValidServerKeyAndCertificateShouldReturnTrue() {
        val inputStream =
            InstrumentationRegistry.getInstrumentation().context.assets.open("credentials.json")

        val credentials = InputStreamReader(inputStream).use { reader ->
            Gson().fromJson(reader, Credentials::class.java)
        }

        val isCertificateValid = sut?.validate(credentials.publicKey, credentials.certificate) ?: false
        assert(isCertificateValid)
    }
}
