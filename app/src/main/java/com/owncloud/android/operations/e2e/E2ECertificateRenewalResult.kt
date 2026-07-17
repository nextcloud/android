/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import androidx.annotation.StringRes

sealed interface E2ECertificateRenewalResult {
    data object Success : E2ECertificateRenewalResult

    data class Failure(@StringRes val messageId: Int) : E2ECertificateRenewalResult
}
