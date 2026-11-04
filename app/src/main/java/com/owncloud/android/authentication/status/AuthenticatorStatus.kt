/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.status

data class AuthenticatorStatus(val icon: Int, val text: String?) {
    val isNone: Boolean
        get() = this == NONE

    companion object {
        const val NO_ICON = 0
        val NONE = AuthenticatorStatus(NO_ICON, "")
    }
}
