/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.model

sealed interface LoginFlowState {
    data object Idle : LoginFlowState

    data object RequestingSession : LoginFlowState

    data class AwaitingApproval(val session: LoginFlowSession) : LoginFlowState

    data object Completed : LoginFlowState

    data class Failed(val reason: LoginFlowFailure) : LoginFlowState
}
