/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.composeActivity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
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
import com.nextcloud.client.assistant.conversation.ConversationViewModel
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepositoryImpl
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepositoryImpl
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepositoryImpl
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.ClientIntegrationScreen
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityComposeBinding
import com.owncloud.android.ui.activity.DrawerActivity

class ComposeActivity : DrawerActivity() {

    lateinit var binding: ActivityComposeBinding
    private val composeViewModel: ComposeViewModel by viewModels()

    companion object {
        const val DESTINATION = "DESTINATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination =
            intent.getParcelableArgument(DESTINATION, ComposeDestination::class.java)
                ?: ComposeDestination.getAssistantScreen(this)

        setupActivityUIFor(destination)

        binding.composeView.setContent {
            MaterialTheme(
                colorScheme = viewThemeUtils.getColorScheme(this),
                content = {
                    Content(destination)
                }
            )
        }

        processText(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processText(intent)
    }

    private fun processText(intent: Intent) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text.isNullOrEmpty()) {
            return
        }

        composeViewModel.updateSelectedText(text.toString())
    }

    override fun getMenuItemId(): Int = R.id.nav_assistant

    override fun onResume() {
        super.onResume()
        highlightNavigationViewItem(menuItemId)
    }

    private fun setupActivityUIFor(destination: ComposeDestination) {
        if (destination is ComposeDestination.AssistantScreen) {
            setupDrawer(menuItemId)
            setupToolbarShowOnlyMenuButtonAndTitle(destination.title) {
                openDrawer()
            }
        } else {
            setSupportActionBar(null)
            findViewById<View?>(R.id.appbar)?.let {
                it.visibility = View.GONE
            }
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

        when (currentScreen) {
            is ComposeDestination.AssistantScreen -> {
                val dao = NextcloudDatabase.instance().assistantDao()
                val sessionId = (currentScreen as? ComposeDestination.AssistantScreen)?.sessionId
                val client = nextcloudClient ?: return
                val optionalCapability = capabilities
                if (optionalCapability.isEmpty) {
                    return
                }
                val capability = optionalCapability.get()

                AssistantScreen(
                    composeViewModel = composeViewModel,
                    viewModel = AssistantViewModel(
                        accountName = userAccountManager.user.accountName,
                        remoteRepository = AssistantRemoteRepositoryImpl(client, capability),
                        localRepository = AssistantLocalRepositoryImpl(dao),
                        sessionIdArg = sessionId
                    ),
                    conversationViewModel = ConversationViewModel(
                        remoteRepository = ConversationRemoteRepositoryImpl(client)
                    ),
                    activity = this,
                    capability = capability
                )
            }

            is ComposeDestination.ClientIntegrationScreen -> {
                binding.bottomNavigation.visibility = View.GONE
                val integrationScreen = (currentScreen as ComposeDestination.ClientIntegrationScreen)
                ClientIntegrationScreen(integrationScreen.data, nextcloudClient?.baseUri.toString())
            }

            else -> Unit
        }
    }
}
