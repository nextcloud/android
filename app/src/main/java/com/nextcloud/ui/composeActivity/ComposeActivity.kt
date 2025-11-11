/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.composeActivity

import android.os.Bundle
import android.view.MenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.client.assistant.conversation.ConversationScreen
import com.nextcloud.client.assistant.conversation.ConversationViewModel
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepositoryImpl
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepositoryImpl
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepositoryImpl
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityComposeBinding
import com.owncloud.android.ui.activity.DrawerActivity

class ComposeActivity : DrawerActivity() {

    lateinit var binding: ActivityComposeBinding

    companion object {
        const val DESTINATION = "DESTINATION"
        const val TITLE = "TITLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destinationId = intent.getIntExtra(DESTINATION, -1)
        val titleId = intent.getIntExtra(TITLE, R.string.empty)

        setupDrawer()

        setupToolbarShowOnlyMenuButtonAndTitle(getString(titleId)) {
            openDrawer()
        }

        binding.composeView.setContent {
            MaterialTheme(
                colorScheme = viewThemeUtils.getColorScheme(this),
                content = {
                    Content(ComposeDestination.fromId(destinationId))
                }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            toggleDrawer()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @Composable
    private fun Content(destination: ComposeDestination) {
        val currentScreen by ComposeNavigation.currentScreen.collectAsState()
        var nextcloudClient by remember { mutableStateOf<NextcloudClient?>(null) }

        LaunchedEffect(Unit) {
            ComposeNavigation.navigate(destination)
            nextcloudClient = clientRepository.getNextcloudClient()
        }

        binding.bottomNavigation.menu.findItem(R.id.nav_assistant).run {
            isChecked = true
        }

        when(currentScreen) {
            is ComposeDestination.AssistantScreen -> {
                val dao = NextcloudDatabase.instance().assistantDao()
                val sessionId = (currentScreen as? ComposeDestination.AssistantScreen)?.sessionId

                nextcloudClient?.let { client ->
                    AssistantScreen(
                        viewModel = AssistantViewModel(
                            accountName = userAccountManager.user.accountName,
                            remoteRepository = AssistantRemoteRepositoryImpl(client, capabilities),
                            localRepository = AssistantLocalRepositoryImpl(dao)
                        ),
                        activity = this,
                        capability = capabilities,
                        sessionIdArg = sessionId
                    )
                }
            }
            ComposeDestination.ConversationScreen -> {
                nextcloudClient?.let { client ->
                    ConversationScreen(viewModel = ConversationViewModel(
                        remoteRepository = ConversationRemoteRepositoryImpl(client)
                    ))
                }
            }
            else -> Unit
        }
    }
}
