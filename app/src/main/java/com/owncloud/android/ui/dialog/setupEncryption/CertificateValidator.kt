/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.setupEncryption

import javax.inject.Inject

class CertificateValidator @Inject constructor() {

    /**
     * Validates certificate with given public key
     *
     * @param serverPublicKeyString Public key in PEM format
     * @param certificate Certificate in PEM format
     */
    fun validate(serverPublicKeyString: String, certificate: String): Boolean =
        verifyCertificate(serverPublicKeyString, certificate)

    private external fun verifyCertificate(serverPublicKeyPem: String, certificatePem: String): Boolean

    companion object {
        init {
            System.loadLibrary("certificate_validator")
        }
    }
}
