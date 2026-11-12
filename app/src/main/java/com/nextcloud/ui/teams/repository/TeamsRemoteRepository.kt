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
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TeamsRemoteRepository @Inject constructor(private val clientRepository: ClientRepository) : TeamsRepository {

    override suspend fun getTeams(): RemoteOperationResult<List<Team>> = execute(GetTeamsRemoteOperation())

    override suspend fun createTeam(name: String): RemoteOperationResult<Team> =
        execute(CreateTeamRemoteOperation(name))

    private suspend fun <T> execute(operation: RemoteOperation<T>): RemoteOperationResult<T> {
        val client = clientRepository.getNextcloudClient() ?: return RemoteOperationResult(ResultCode.ACCOUNT_NOT_FOUND)
        return withContext(Dispatchers.IO) { operation.execute(client) }
    }
}
