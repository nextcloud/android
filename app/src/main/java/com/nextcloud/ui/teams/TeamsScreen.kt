/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.teams.component.CreateTeamDialog
import com.nextcloud.ui.teams.component.TeamInvitationListItem
import com.nextcloud.ui.teams.component.TeamListItem
import com.nextcloud.ui.teams.component.TeamsErrorMessage
import com.nextcloud.ui.teams.component.TeamsMessage
import com.nextcloud.ui.teams.component.TeamsSearchBar
import com.nextcloud.ui.teams.model.TeamsUiState
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.owncloud.android.R

private const val SECTION_HEADER_CONTENT_TYPE = "section_header"

@Composable
fun TeamsScreen(viewModel: TeamsViewModel, client: NextcloudClient, onTeamClick: (Team) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val createTeamName by viewModel.createTeamName.collectAsStateWithLifecycle()
    val createTeamStatus by viewModel.createTeamStatus.collectAsStateWithLifecycle()
    val respondingTeamIds by viewModel.respondingTeamIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.errorMessages.collect { snackbarHostState.showSnackbar(resources.getString(it)) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.teams_create)) },
                icon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
                onClick = { viewModel.updateCreateTeamStatus("") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                TeamsUiState.Loading -> CircularProgressIndicator()

                is TeamsUiState.Error -> TeamsErrorMessage(state.messageId, onRetry = viewModel::retry)

                is TeamsUiState.Content -> TeamsContent(searchQuery, viewModel::updateSearchQuery, state.isEmpty()) {
                    teamSection(R.string.teams_invitations, state.invitations) { team ->
                        TeamInvitationListItem(team, client, isResponding = team.id in respondingTeamIds) {
                            viewModel.respondToInvitation(team, it)
                        }
                    }
                    teamSection(R.string.teams_my_teams, state.myTeams) { team ->
                        TeamListItem(team, client) { onTeamClick(team) }
                    }
                }
            }
        }
    }

    createTeamName?.let { name ->
        CreateTeamDialog(
            name = name,
            status = createTeamStatus,
            onNameChange = viewModel::updateCreateTeamStatus,
            onDismiss = { viewModel.updateCreateTeamStatus(null) },
            onCreate = viewModel::createTeam
        )
    }
}

@Composable
private fun TeamsContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isEmpty: Boolean,
    sections: LazyListScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TeamsSearchBar(searchQuery, R.string.teams_search_hint, onSearchQueryChange)

        if (isEmpty) {
            val messageId = if (searchQuery.isBlank()) R.string.teams_empty else R.string.teams_search_no_results
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TeamsMessage(messageId)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), content = sections)
        }
    }
}

private fun LazyListScope.teamSection(
    @StringRes titleId: Int,
    teams: List<Team>,
    itemContent: @Composable (Team) -> Unit
) {
    if (teams.isEmpty()) {
        return
    }

    item(key = titleId, contentType = SECTION_HEADER_CONTENT_TYPE) { SectionHeader(titleId) }
    items(teams, key = { it.id }, contentType = { titleId }) { itemContent(it) }
}

@Composable
private fun SectionHeader(@StringRes titleId: Int) {
    Text(
        text = stringResource(titleId),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.standard_padding),
            end = dimensionResource(R.dimen.standard_padding),
            top = dimensionResource(R.dimen.standard_padding),
            bottom = dimensionResource(R.dimen.standard_half_padding)
        )
    )
}
