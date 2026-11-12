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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextcloud.ui.teams.detail.component.AddTeamMemberDialog
import com.nextcloud.ui.teams.detail.component.TeamDetailListStateContent
import com.nextcloud.ui.teams.detail.component.TeamMemberListItem
import com.nextcloud.ui.teams.detail.component.TeamResourceListItem
import com.nextcloud.ui.teams.detail.model.TeamResourceTarget
import com.nextcloud.ui.teams.detail.model.TeamDetailTab
import com.nextcloud.ui.teams.detail.viewmodel.TeamDetailViewModel
import com.owncloud.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel,
    title: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenResource: (TeamResourceTarget) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val addMemberQuery by viewModel.addMemberQuery.collectAsStateWithLifecycle()
    val addMemberStatus by viewModel.addMemberStatus.collectAsStateWithLifecycle()
    val memberSuggestions by viewModel.memberSuggestions.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.team_settings))
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        floatingActionButton = {
            if (selectedTab == TeamDetailTab.MEMBERS) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.team_add_member)) },
                    icon = { Icon(painterResource(R.drawable.ic_account_plus), contentDescription = null) },
                    onClick = viewModel::openAddMemberDialog
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

                TeamDetailTab.MEMBERS -> TeamDetailListStateContent(
                    state = members,
                    emptyMessageId = R.string.team_members_empty,
                    onRetry = viewModel::retryMembers,
                    key = { it.singleId }
                ) { member ->
                    TeamMemberListItem(member)
                }
            }
        }
    }

    addMemberQuery?.let { query ->
        AddTeamMemberDialog(
            query = query,
            status = addMemberStatus,
            suggestions = memberSuggestions,
            onQueryChange = viewModel::updateAddMemberQuery,
            onSelect = viewModel::addMember,
            onDismiss = viewModel::dismissAddMemberDialog
        )
    }
}
