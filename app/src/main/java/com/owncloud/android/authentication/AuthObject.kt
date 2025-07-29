/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.authentication

data class AuthObject(
    val poll: Poll,
    val login: String,
)

data class Poll(
    val token: String,
    val endpoint: String,
)
