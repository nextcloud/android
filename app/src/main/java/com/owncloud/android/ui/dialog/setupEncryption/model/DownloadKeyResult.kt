/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.setupEncryption.model

import com.owncloud.android.R

sealed class DownloadKeyResult(open val descriptionId: Int? = null) {
    data class CertificateVerificationFailed(
        override val descriptionId: Int = R.string.end_to_end_encryption_certificate_verification_failed
    ) : DownloadKeyResult(descriptionId)

    data class ServerPublicKeyUnavailable(
        override val descriptionId: Int = R.string.end_to_end_encryption_server_public_key_unavailable
    ) : DownloadKeyResult(descriptionId)

    data class ServerPrivateKeyUnavailable(
        override val descriptionId: Int = R.string.end_to_end_encryption_server_private_key_unavailable
    ) : DownloadKeyResult(descriptionId)

    data class CertificateUnavailable(
        override val descriptionId: Int = R.string.end_to_end_encryption_certificate_unavailable
    ) : DownloadKeyResult(descriptionId)

    data class UnexpectedError(
        override val descriptionId: Int = R.string.end_to_end_encryption_unexpected_error_occurred
    ) : DownloadKeyResult(descriptionId)

    data object GeneratePassphraseSendCSR : DownloadKeyResult()

    data class Success(val privateKey: String) : DownloadKeyResult()
}
