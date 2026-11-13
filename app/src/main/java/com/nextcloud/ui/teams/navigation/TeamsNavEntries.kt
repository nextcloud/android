/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.nextcloud.client.account.User
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.ui.compose.navigation.ComposeDestination
import com.nextcloud.ui.compose.navigation.ComposeNavigator
import com.nextcloud.ui.teams.TeamsScreen
import com.nextcloud.ui.teams.detail.TeamDetailScreen
import com.nextcloud.ui.teams.detail.TeamResourceOpener
import com.nextcloud.ui.teams.detail.viewmodel.TeamDetailViewModel
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel

fun EntryProviderScope<ComposeDestination>.teamsEntries(
    navigator: ComposeNavigator,
    vmFactory: ViewModelFactory,
    user: User
) {
    entry<ComposeDestination.TeamsScreen> {
        TeamsScreen(
            viewModel = viewModel<TeamsViewModel>(factory = vmFactory),
            onTeamClick = { team ->
                navigator.navigate(ComposeDestination.TeamDetailScreen(team.id, team.displayName, team.url))
            }
        )
    }

    entry<ComposeDestination.TeamDetailScreen> { screen ->
        val activity = checkNotNull(LocalActivity.current)
        val resourceOpener = remember(activity) {
            TeamResourceOpener(activity, user) { page ->
                navigator.navigate(ComposeDestination.WebPageScreen(page.title, page.url))
            }
        }

        TeamDetailScreen(
            viewModel = teamDetailViewModel(vmFactory, screen.teamId),
            title = screen.title,
            onBack = navigator::goBack,
            onOpenSettings = {
                navigator.navigate(ComposeDestination.WebPageScreen(screen.title, screen.settingsUrl))
            },
            onOpenResource = resourceOpener::open
        )
    }
}

@Composable
private fun teamDetailViewModel(vmFactory: ViewModelFactory, teamId: String): TeamDetailViewModel {
    val owner = LocalViewModelStoreOwner.current as? HasDefaultViewModelProviderFactory
    val extras = MutableCreationExtras(owner?.defaultViewModelCreationExtras ?: CreationExtras.Empty).apply {
        set(DEFAULT_ARGS_KEY, bundleOf(TeamDetailViewModel.KEY_TEAM_ID to teamId))
    }
    return viewModel(factory = vmFactory, extras = extras)
}
