/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nextcloud.client.account.User
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.ClientIntegrationScreen
import com.nextcloud.ui.compose.component.webView.WebPageScreen
import com.nextcloud.ui.teams.navigation.teamsEntries

@Composable
fun ComposeNavHost(
    navigator: ComposeNavigator,
    vmFactory: ViewModelFactory,
    user: User,
    nextcloudClient: NextcloudClient?,
    assistantContent: @Composable (ComposeDestination.AssistantScreen) -> Unit
) {
    NavDisplay(
        backStack = navigator.backStack,
        onBack = navigator::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<ComposeDestination.AssistantScreen> { screen -> assistantContent(screen) }

            entry<ComposeDestination.ClientIntegrationScreen> { screen ->
                ClientIntegrationScreen(screen.data, nextcloudClient?.baseUri.toString())
            }

            teamsEntries(navigator, vmFactory, user)

            entry<ComposeDestination.WebPageScreen> { screen ->
                WebPageScreen(screen.title, screen.url, nextcloudClient, onClose = navigator::goBack)
            }
        }
    )
}
