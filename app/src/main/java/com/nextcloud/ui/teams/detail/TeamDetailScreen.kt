/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.teams.component.TeamsSearchBar
import com.nextcloud.ui.teams.detail.component.AddTeamMemberDialog
import com.nextcloud.ui.teams.detail.component.TeamAccessFilterRow
import com.nextcloud.ui.teams.detail.component.TeamDetailListStateContent
import com.nextcloud.ui.teams.detail.component.TeamDetailTopBar
import com.nextcloud.ui.teams.detail.component.TeamMemberListItem
import com.nextcloud.ui.teams.detail.component.TeamResourceListItem
import com.nextcloud.ui.teams.detail.model.TeamDetailListUiState
import com.nextcloud.ui.teams.detail.model.TeamDetailTab
import com.nextcloud.ui.teams.detail.model.TeamResourceTarget
import com.nextcloud.ui.teams.detail.viewmodel.TeamDetailViewModel
import com.nextcloud.ui.teams.model.TeamAccess
import com.owncloud.android.R

@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel,
    client: NextcloudClient,
    title: String,
    settingsUrl: String,
    access: TeamAccess,
    onBack: () -> Unit,
    onOpenResource: (TeamResourceTarget) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val memberQuery by viewModel.memberQuery.collectAsStateWithLifecycle()
    val memberAccessFilter by viewModel.memberAccessFilter.collectAsStateWithLifecycle()
    val addMemberQuery by viewModel.addMemberQuery.collectAsStateWithLifecycle()
    val addMemberStatus by viewModel.addMemberStatus.collectAsStateWithLifecycle()
    val memberSuggestions by viewModel.memberSuggestions.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TeamDetailTopBar(title, onBack) {
                if (access.isManager && settingsUrl.isNotBlank()) {
                    IconButton(onClick = { onOpenResource(TeamResourceTarget.WebPage(settingsUrl, title)) }) {
                        Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.team_settings))
                    }
                }
            }
        },
        floatingActionButton = {
            if (access.isManager && selectedTab == TeamDetailTab.MEMBERS) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.team_add_member)) },
                    icon = { Icon(painterResource(R.drawable.ic_account_plus), contentDescription = null) },
                    onClick = { viewModel.updateMemberStatus("") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                TeamDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(stringResource(tab.titleId)) }
                    )
                }
            }

            when (selectedTab) {
                TeamDetailTab.RESOURCES -> TeamDetailListStateContent(
                    state = resources,
                    emptyMessageId = R.string.team_resources_empty,
                    onRetry = viewModel::retryResources,
                    key = { "${it.provider.id}:${it.id}" }
                ) { resource ->
                    TeamResourceListItem(resource) { onOpenResource(viewModel.targetFor(resource)) }
                }

                TeamDetailTab.MEMBERS -> MembersTab(
                    state = members,
                    query = memberQuery,
                    accessFilter = memberAccessFilter,
                    client = client,
                    viewModel = viewModel
                )
            }
        }
    }

    addMemberQuery?.let { query ->
        AddTeamMemberDialog(
            query = query,
            status = addMemberStatus,
            suggestions = memberSuggestions,
            onQueryChange = viewModel::updateMemberStatus,
            onSelect = viewModel::addMember,
            onDismiss = { viewModel.updateMemberStatus(null) }
        )
    }
}

@Composable
private fun MembersTab(
    state: TeamDetailListUiState<TeamMember>,
    query: String,
    accessFilter: TeamAccess?,
    client: NextcloudClient,
    viewModel: TeamDetailViewModel
) {
    val hasActiveFilters = query.isNotBlank() || accessFilter != null
    val emptyMessageId = if (hasActiveFilters) R.string.team_members_search_no_results else R.string.team_members_empty

    Column(modifier = Modifier.fillMaxSize()) {
        TeamsSearchBar(query, R.string.team_members_search_hint, viewModel::updateMemberQuery)
        TeamAccessFilterRow(accessFilter, viewModel::updateMemberAccessFilter)

        TeamDetailListStateContent(
            state = state,
            emptyMessageId = emptyMessageId,
            onRetry = viewModel::retryMembers,
            key = { it.singleId }
        ) { member ->
            TeamMemberListItem(member, client)
        }
    }
}
