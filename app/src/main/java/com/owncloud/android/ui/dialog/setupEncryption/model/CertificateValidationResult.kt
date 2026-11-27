/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.setupEncryption.model

import androidx.annotation.StringRes
import com.owncloud.android.R

enum class CertificateValidationResult(@param:StringRes val descriptionId: Int?) {
    VALID(null),
    INVALID_SERVER_PUBLIC_KEY(R.string.e2ee_certificate_invalid_server_key),
    INVALID_CERTIFICATE(R.string.e2ee_certificate_invalid_format),
    EXPIRED(R.string.e2ee_certificate_expired),
    NOT_YET_VALID(R.string.e2ee_certificate_not_yet_valid),
    INVALID_SIGNATURE(R.string.e2ee_certificate_invalid_signature),
    UNSUPPORTED_ALGORITHM(R.string.e2ee_certificate_unsupported_algorithm),
    VERIFICATION_FAILED(R.string.e2ee_certificate_verification_failed)
}
