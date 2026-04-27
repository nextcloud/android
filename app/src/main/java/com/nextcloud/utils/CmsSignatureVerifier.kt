/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

/**
 * JNI bridge that delegates CMS signature verification to native OpenSSL.
 *
 * The native implementation mirrors the iOS `verifySignatureCMS` logic:
 * - Parses the DER-encoded CMS structure
 * - Verifies the detached signature without CA-chain validation (CMS_NO_SIGNER_CERT_VERIFY)
 * - Matches the resulting signer identity against each supplied PEM certificate
 *   (equivalent to `CMS_SignerInfo_cert_cmp`)
 */
class CmsSignatureVerifier {
    /**
     * @param cmsData      DER-encoded CMS ContentInfo (detached, without embedded content)
     * @param messageData  The raw content bytes that were signed
     * @param certificates PEM-encoded X.509 certificates to match against the CMS signer
     * @return `true` if the signature is cryptographically valid and the signer matches
     *         at least one of the supplied certificates
     */
    external fun verifySignedData(cmsData: ByteArray, messageData: ByteArray, certificates: Array<String>): Boolean

    companion object {
        init {
            System.loadLibrary("cms_verifier")
        }
    }
}
