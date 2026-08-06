/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginSessionResponse(val login: String = "", val poll: LoginPoll = LoginPoll())