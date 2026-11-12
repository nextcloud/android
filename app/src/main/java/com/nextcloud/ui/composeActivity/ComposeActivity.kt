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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
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
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.ui.ClientIntegrationScreen
import com.nextcloud.ui.composeComponents.webView.WebPageScreen
import com.nextcloud.ui.teams.detail.TeamResourceOpener
import com.nextcloud.ui.teams.detail.TeamDetailScreen
import com.nextcloud.ui.teams.detail.model.TeamResourceTarget
import com.nextcloud.ui.teams.detail.viewmodel.TeamDetailViewModel
import com.nextcloud.ui.teams.TeamsScreen
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityComposeBinding
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import javax.inject.Inject

class ComposeActivity : DrawerActivity() {

    @Inject
    lateinit var vmFactory: ViewModelFactory

    lateinit var binding: ActivityComposeBinding
    private val composeViewModel: ComposeViewModel by viewModels()

    private val destination: ComposeDestination by lazy {
        intent.getParcelableArgument(DESTINATION, ComposeDestination::class.java)
            ?: ComposeDestination.getAssistantScreen(this)
    }

    companion object {
        const val DESTINATION = "DESTINATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActivityUIFor(destination)

        binding.composeView.setContent {
            MaterialTheme(
                colorScheme = viewThemeUtils.getColorScheme(this),
                content = {
                    Content()
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

    override val menuItemId: Int
        get() = when (destination) {
            is ComposeDestination.TeamsScreen,
            is ComposeDestination.TeamDetailScreen,
            is ComposeDestination.WebPageScreen -> Menu.NONE

            else -> R.id.nav_assistant
        }

    override fun onResume() {
        super.onResume()
        highlightNavigationViewItem(menuItemId)
    }

    private fun setupActivityUIFor(destination: ComposeDestination) {
        when (destination) {
            is ComposeDestination.AssistantScreen -> setupDrawerScreen(destination.title)
            is ComposeDestination.TeamsScreen -> setupDrawerScreen(destination.title)
            is ComposeDestination.ClientIntegrationScreen,
            is ComposeDestination.TeamDetailScreen,
            is ComposeDestination.WebPageScreen -> hideActionBar()
        }
    }

    private fun hideActionBar() {
        setSupportActionBar(null)
        findViewById<View?>(R.id.appbar)?.let {
            it.visibility = View.GONE
        }
    }

    private fun openTeam(team: Team) {
        val destination = ComposeDestination.TeamDetailScreen(team.id, team.displayName, team.url)
        val intent = Intent(this, ComposeActivity::class.java)
            .putExtra(DESTINATION, destination)
            .putExtra(TeamDetailViewModel.KEY_TEAM_ID, team.id)
        startActivity(intent)
    }

    private fun setupDrawerScreen(title: String) {
        setupDrawer(menuItemId)
        showMenuAndTitleActionBar(title) {
            openDrawer()
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
    private fun Content() {
        var nextcloudClient by remember { mutableStateOf<NextcloudClient?>(null) }

        LaunchedEffect(Unit) {
            nextcloudClient = clientRepository.getNextcloudClient()
        }

        when (val screen = destination) {
            is ComposeDestination.AssistantScreen -> {
                binding.bottomNavigation.menu.findItem(R.id.nav_assistant).run {
                    isChecked = true
                }

                val dao = NextcloudDatabase.instance().assistantDao()
                val client = nextcloudClient ?: return
                val optionalCapability = capabilities
                if (optionalCapability.isEmpty) {
                    return
                }
                val capability = optionalCapability.get()
                val remoteRepository = AssistantRemoteRepositoryImpl(client, capability)

                AssistantScreen(
                    composeViewModel = composeViewModel,
                    viewModel = AssistantViewModel(
                        accountName = userAccountManager.user.accountName,
                        remoteRepository = remoteRepository,
                        localRepository = AssistantLocalRepositoryImpl(dao),
                        sessionIdArg = screen.sessionId
                    ),
                    chatViewModel = ChatViewModel(remoteRepository),
                    conversationViewModel = ConversationViewModel(
                        remoteRepository = ConversationRemoteRepositoryImpl(client)
                    ),
                    activity = this,
                    capability = capability
                )
            }

            is ComposeDestination.ClientIntegrationScreen -> {
                binding.bottomNavigation.visibility = View.GONE
                ClientIntegrationScreen(screen.data, nextcloudClient?.baseUri.toString())
            }

            is ComposeDestination.TeamsScreen -> {
                val viewModel = remember {
                    ViewModelProvider(this, vmFactory)[TeamsViewModel::class.java]
                }
                TeamsScreen(viewModel, onTeamClick = ::openTeam)
            }

            is ComposeDestination.TeamDetailScreen -> {
                binding.bottomNavigation.visibility = View.GONE
                val viewModel = remember {
                    ViewModelProvider(this, vmFactory)[TeamDetailViewModel::class.java]
                }
                val opener = remember { TeamResourceOpener(this, userAccountManager.user) }
                TeamDetailScreen(
                    viewModel = viewModel,
                    title = screen.title,
                    onBack = ::finish,
                    onOpenSettings = { opener.open(TeamResourceTarget.WebPage(screen.settingsUrl, screen.title)) },
                    onOpenResource = opener::open
                )
            }

            is ComposeDestination.WebPageScreen -> {
                binding.bottomNavigation.visibility = View.GONE
                WebPageScreen(screen.title, screen.url, nextcloudClient, onClose = ::finish)
            }
        }
    }
}
