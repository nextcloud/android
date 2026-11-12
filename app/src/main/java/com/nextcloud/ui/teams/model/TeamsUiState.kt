/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.model

import com.nextcloud.android.lib.resources.teams.model.Team

sealed interface TeamsUiState {
    data object Loading : TeamsUiState

    data object Error : TeamsUiState

    data class Content(val myTeams: List<Team>, val otherTeams: List<Team>) : TeamsUiState
}
