/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.repository

import com.nextcloud.android.lib.resources.teams.model.Team

interface TeamsRepository {
    suspend fun getTeams(): List<Team>?

    suspend fun createTeam(name: String): Team?
}
