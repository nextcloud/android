/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.model

import androidx.annotation.StringRes
import com.nextcloud.android.lib.resources.teams.model.Team

sealed interface TeamsUiState {
    data object Loading : TeamsUiState

    data class Error(@StringRes val messageId: Int) : TeamsUiState

    data class Content(val myTeams: List<Team>, val otherTeams: List<Team>) : TeamsUiState
}
