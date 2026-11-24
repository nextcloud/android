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

    data class Content(val invitations: List<Team>, val myTeams: List<Team>) : TeamsUiState {
        fun isEmpty(): Boolean = invitations.isEmpty() && myTeams.isEmpty()

        fun matching(query: String): Content {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isEmpty()) {
                return this
            }

            val matches: (Team) -> Boolean = { it.displayName.contains(trimmedQuery, ignoreCase = true) }
            return Content(invitations.filter(matches), myTeams.filter(matches))
        }
    }
}
