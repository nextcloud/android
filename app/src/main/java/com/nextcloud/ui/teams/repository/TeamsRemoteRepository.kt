/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.repository

import com.nextcloud.android.lib.resources.teams.get.team.GetTeamsRemoteOperation
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.android.lib.resources.teams.post.team.CreateTeamRemoteOperation
import com.nextcloud.repository.ClientRepository
import com.nextcloud.utils.extensions.execute
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import javax.inject.Inject

class TeamsRemoteRepository @Inject constructor(private val clientRepository: ClientRepository) : TeamsRepository {

    override suspend fun getTeams(): RemoteOperationResult<List<Team>> =
        clientRepository.execute(GetTeamsRemoteOperation())

    override suspend fun createTeam(name: String): RemoteOperationResult<Team> =
        clientRepository.execute(CreateTeamRemoteOperation(name))
}
