/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.text

data class RichSubjectParam(val type: String?, val id: String?, val name: String?, val onClick: (() -> Unit)? = null) {
    val isMention: Boolean
        get() = type == USER_TYPE

    companion object {
        private const val USER_TYPE = "user"
    }
}
