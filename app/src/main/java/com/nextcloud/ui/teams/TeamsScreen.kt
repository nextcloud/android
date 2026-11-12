/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.ui.teams.component.CreateTeamDialog
import com.nextcloud.ui.teams.model.TeamsUiState
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.owncloud.android.R

@Composable
fun TeamsScreen(viewModel: TeamsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val createTeamName by viewModel.createTeamName.collectAsStateWithLifecycle()
    val createTeamStatus by viewModel.createTeamStatus.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.teams_create)) },
                icon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
                onClick = viewModel::openCreateTeamDialog
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
                is TeamsUiState.Error -> TeamsError(state.messageId, onRetry = viewModel::retry)
                is TeamsUiState.Content -> TeamsContent(state)
            }
        }
    }

    createTeamName?.let { name ->
        CreateTeamDialog(
            name = name,
            status = createTeamStatus,
            onNameChange = viewModel::updateCreateTeamName,
            onDismiss = viewModel::dismissCreateTeamDialog,
            onCreate = viewModel::createTeam
        )
    }
}

@Composable
private fun TeamsContent(state: TeamsUiState.Content) {
    if (state.myTeams.isEmpty() && state.otherTeams.isEmpty()) {
        Text(
            text = stringResource(R.string.teams_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        teamSection(R.string.teams_my_teams, state.myTeams)
        teamSection(R.string.teams_other_teams, state.otherTeams)
    }
}

private fun LazyListScope.teamSection(@StringRes titleId: Int, teams: List<Team>) {
    if (teams.isEmpty()) {
        return
    }

    item(key = titleId) { SectionHeader(titleId) }
    items(teams, key = { it.id }) { TeamListItem(it) }
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

@Composable
private fun TeamListItem(team: Team) {
    ListItem(
        headlineContent = {
            Text(text = team.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(pluralStringResource(R.plurals.teams_member_count, team.population, team.population))
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_circles),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun TeamsError(@StringRes messageId: Int, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding))
    ) {
        Text(
            text = stringResource(messageId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
