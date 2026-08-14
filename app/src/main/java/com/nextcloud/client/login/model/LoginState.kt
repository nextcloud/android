/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.model

sealed interface LoginState {
    data object Idle : LoginState

    data object RequestingSession : LoginState

    data class AwaitingApproval(val session: LoginSession) : LoginState

    data object Completed : LoginState

    data class Failed(val reason: LoginFailure) : LoginState
}
