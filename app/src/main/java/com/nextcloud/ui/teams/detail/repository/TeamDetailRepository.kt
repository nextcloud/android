/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.repository

import com.nextcloud.android.lib.resources.collectives.model.CollectivesUserFolder
import com.nextcloud.android.lib.resources.teams.get.folder.model.TeamFolder
import com.nextcloud.android.lib.resources.teams.get.resources.model.TeamResource
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.android.lib.resources.teams.post.members.model.NewTeamMember
import com.nextcloud.ui.teams.detail.model.TeamMemberSuggestion
import com.owncloud.android.lib.common.operations.RemoteOperationResult

interface TeamDetailRepository {
    suspend fun getResources(teamId: String): RemoteOperationResult<List<TeamResource>>

    suspend fun getFolder(teamId: String): RemoteOperationResult<TeamFolder>

    suspend fun getCollectivesUserFolder(): RemoteOperationResult<CollectivesUserFolder>

    suspend fun getMembers(teamId: String): RemoteOperationResult<List<TeamMember>>

    suspend fun addMember(teamId: String, member: NewTeamMember): RemoteOperationResult<Void>

    suspend fun searchMemberSuggestions(query: String): List<TeamMemberSuggestion>
}
