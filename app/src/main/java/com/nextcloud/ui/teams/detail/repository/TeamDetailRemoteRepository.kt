/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.repository

import com.nextcloud.android.lib.resources.collectives.GetCollectivesUserFolderRemoteOperation
import com.nextcloud.android.lib.resources.collectives.model.CollectivesUserFolder
import com.nextcloud.android.lib.resources.teams.get.folder.GetTeamFolderRemoteOperation
import com.nextcloud.android.lib.resources.teams.get.folder.model.TeamFolder
import com.nextcloud.android.lib.resources.teams.get.members.GetTeamMembersRemoteOperation
import com.nextcloud.android.lib.resources.teams.get.resources.GetTeamResourcesRemoteOperation
import com.nextcloud.android.lib.resources.teams.get.resources.model.TeamResource
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.android.lib.resources.teams.model.TeamMemberType
import com.nextcloud.android.lib.resources.teams.post.members.AddTeamMembersRemoteOperation
import com.nextcloud.android.lib.resources.teams.post.members.model.NewTeamMember
import com.nextcloud.repository.ClientRepository
import com.nextcloud.ui.teams.detail.model.TeamMemberSuggestion
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation
import com.owncloud.android.lib.resources.shares.ShareType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class TeamDetailRemoteRepository @Inject constructor(private val clientRepository: ClientRepository) :
    TeamDetailRepository {

    override suspend fun getResources(teamId: String): RemoteOperationResult<List<TeamResource>> =
        execute(GetTeamResourcesRemoteOperation(teamId))

    override suspend fun getFolder(teamId: String): RemoteOperationResult<TeamFolder> =
        execute(GetTeamFolderRemoteOperation(teamId))

    override suspend fun getCollectivesUserFolder(): RemoteOperationResult<CollectivesUserFolder> =
        execute(GetCollectivesUserFolderRemoteOperation())

    override suspend fun getMembers(teamId: String): RemoteOperationResult<List<TeamMember>> =
        execute(GetTeamMembersRemoteOperation(teamId))

    override suspend fun addMember(teamId: String, member: NewTeamMember): RemoteOperationResult<Void> =
        execute(AddTeamMembersRemoteOperation(teamId, listOf(member)))

    override suspend fun searchMemberSuggestions(query: String): List<TeamMemberSuggestion> {
        val client = clientRepository.getOwncloudClient() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val result = GetShareesRemoteOperation(query, FIRST_PAGE, SUGGESTIONS_PER_PAGE).execute(client)
            if (!result.isSuccess) {
                return@withContext emptyList()
            }
            result.resultData.orEmpty().mapNotNull { it.toTeamMemberSuggestion() }
        }
    }

    private suspend fun <T> execute(operation: RemoteOperation<T>): RemoteOperationResult<T> {
        val client = clientRepository.getNextcloudClient() ?: return RemoteOperationResult(ResultCode.ACCOUNT_NOT_FOUND)
        return withContext(Dispatchers.IO) { operation.execute(client) }
    }

    private fun JSONObject.toTeamMemberSuggestion(): TeamMemberSuggestion? {
        val value = optJSONObject(GetShareesRemoteOperation.NODE_VALUE) ?: return null
        val shareType = ShareType.fromValue(value.optInt(GetShareesRemoteOperation.PROPERTY_SHARE_TYPE))
        val type = TeamMemberType.fromShareType(shareType) ?: return null
        val id = value.optString(GetShareesRemoteOperation.PROPERTY_SHARE_WITH).ifEmpty { return null }
        val label = optString(GetShareesRemoteOperation.PROPERTY_LABEL).ifEmpty { id }
        return TeamMemberSuggestion(label, NewTeamMember(id, type))
    }

    companion object {
        private const val FIRST_PAGE = 1
        private const val SUGGESTIONS_PER_PAGE = 20
    }
}
