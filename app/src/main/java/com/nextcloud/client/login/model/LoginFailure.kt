/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.model

enum class LoginFailure {
    EMPTY_SERVER_URL,
    EMPTY_RESPONSE,
    MALFORMED_RESPONSE,
    TIMED_OUT
}