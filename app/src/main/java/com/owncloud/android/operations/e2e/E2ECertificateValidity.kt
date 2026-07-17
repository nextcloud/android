/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import java.util.Calendar
import java.util.Date

data class E2ECertificateValidity(val notBefore: Date, val notAfter: Date) {
    val isExpired: Boolean
        get() = Date().after(notAfter)

    val isNearExpiry: Boolean
        get() {
            val warningThreshold = Calendar.getInstance().apply {
                time = notAfter
                add(Calendar.MONTH, -NEAR_EXPIRY_WARNING_MONTHS)
            }.time

            return !Date().before(warningThreshold)
        }

    companion object {
        private const val NEAR_EXPIRY_WARNING_MONTHS = 1
    }
}
