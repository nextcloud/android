/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

class CmsSignatureVerifier {
    external fun verifySignedData(cmsData: ByteArray, messageData: ByteArray, certificates: Array<String>): Boolean

    companion object {
        init {
            System.loadLibrary("cms_verifier")
        }
    }
}
