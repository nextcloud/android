/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.navigation

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.client.assistant.chat.ChatViewModel
import com.nextcloud.client.assistant.conversation.ConversationViewModel
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepositoryImpl
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepositoryImpl
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepositoryImpl
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.ClientIntegrationScreen
import com.nextcloud.ui.compose.ComposeActivity
import com.nextcloud.ui.teams.TeamsScreen
import com.nextcloud.ui.teams.detail.TeamDetailScreen
import com.nextcloud.ui.teams.detail.TeamResourceOpener
import com.nextcloud.ui.teams.detail.model.TeamResourceTarget
import com.nextcloud.ui.teams.detail.viewmodel.TeamDetailViewModel
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import kotlin.jvm.optionals.getOrNull

private val noTransition = ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)

@Composable
fun ComposeNavHost(navigator: ComposeNavigator) {
    NavDisplay(
        backStack = navigator.backStack,
        onBack = navigator::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = { noTransition },
        popTransitionSpec = { noTransition },
        predictivePopTransitionSpec = { noTransition },
        entryProvider = entryProvider {
            entry<ComposeDestination.AssistantScreen> { screen ->
                val activity = composeActivity()
                val capability = activity.capabilities.getOrNull() ?: return@entry
                val client = rememberNextcloudClient() ?: return@entry
                val remoteRepository = remember(client, capability) {
                    AssistantRemoteRepositoryImpl(client, capability)
                }

                AssistantScreen(
                    composeViewModel = activity.composeViewModel,
                    viewModel = viewModel {
                        AssistantViewModel(
                            accountName = activity.userAccountManager.user.accountName,
                            remoteRepository = remoteRepository,
                            localRepository = AssistantLocalRepositoryImpl(NextcloudDatabase.instance().assistantDao()),
                            sessionIdArg = screen.sessionId
                        )
                    },
                    chatViewModel = viewModel { ChatViewModel(remoteRepository) },
                    conversationViewModel = viewModel {
                        ConversationViewModel(remoteRepository = ConversationRemoteRepositoryImpl(client))
                    },
                    activity = activity,
                    capability = capability
                )
            }

            entry<ComposeDestination.ClientIntegrationScreen> { screen ->
                val client = rememberNextcloudClient() ?: return@entry

                ClientIntegrationScreen(
                    clientIntegrationUI = screen.data,
                    baseUrl = client.baseUri.toString()
                )
            }

            entry<ComposeDestination.TeamsScreen> {
                TeamsScreen(
                    viewModel = viewModel<TeamsViewModel>(factory = composeActivity().vmFactory),
                    onTeamClick = { team ->
                        navigator.navigate(ComposeDestination.TeamDetailScreen(team.id, team.displayName, team.url))
                    }
                )
            }

            entry<ComposeDestination.TeamDetailScreen> { screen ->
                val activity = composeActivity()
                val resourceOpener = remember(activity) {
                    TeamResourceOpener(activity, activity.userAccountManager.user)
                }

                TeamDetailScreen(
                    viewModel = teamDetailViewModel(activity.vmFactory, screen.teamId),
                    title = screen.title,
                    onBack = navigator::goBack,
                    onOpenSettings = {
                        resourceOpener.open(TeamResourceTarget.WebPage(screen.settingsUrl, screen.title))
                    },
                    onOpenResource = resourceOpener::open
                )
            }
        }
    )
}

@Composable
private fun composeActivity(): ComposeActivity = LocalActivity.current as ComposeActivity

@Composable
private fun rememberNextcloudClient(): NextcloudClient? {
    val activity = composeActivity()
    val client by produceState<NextcloudClient?>(initialValue = null, activity) {
        value = activity.clientRepository.getNextcloudClient()
        if (value == null) {
            SnackbarUtil.show(activity, R.string.account_not_found)
        }
    }
    return client
}

@Composable
private fun teamDetailViewModel(vmFactory: ViewModelFactory, teamId: String): TeamDetailViewModel {
    val owner = LocalViewModelStoreOwner.current as? HasDefaultViewModelProviderFactory
    val extras = MutableCreationExtras(owner?.defaultViewModelCreationExtras ?: CreationExtras.Empty).apply {
        set(DEFAULT_ARGS_KEY, Bundle().apply { putString(TeamDetailViewModel.KEY_TEAM_ID, teamId) })
    }
    return viewModel(factory = vmFactory, extras = extras)
}
