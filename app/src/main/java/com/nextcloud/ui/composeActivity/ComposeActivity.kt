/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.composeActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nextcloud.android.lib.resources.clientintegration.ClientIntegrationUI
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.client.assistant.conversation.ConversationViewModel
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepositoryImpl
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepositoryImpl
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepositoryImpl
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.ClientIntegrationScreen
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityComposeBinding
import com.owncloud.android.ui.activity.DrawerActivity

class ComposeActivity : DrawerActivity() {

    lateinit var binding: ActivityComposeBinding

    companion object {
        const val DESTINATION = "DESTINATION"
        const val TITLE = "TITLE"
        const val TITLE_STRING = "TITLE_STRING"
        const val ARGS_CLIENT_INTEGRATION_UI = "ARGS_ClIENT_INTEGRATION_UI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destinationId = intent.getIntExtra(DESTINATION, -1)
        val titleId = intent.getIntExtra(TITLE, R.string.empty)

        if (title == null || title.isEmpty()) {
            title = getString(intent.getIntExtra(TITLE, R.string.empty))
        }

        if (destination == ComposeDestination.AssistantScreen) {
            setupDrawer()

            setupToolbarShowOnlyMenuButtonAndTitle(title) {
                openDrawer()
            }
        } else {
            setSupportActionBar(null)
            if (findViewById<View?>(R.id.appbar) != null) {
                findViewById<View?>(R.id.appbar)?.visibility = View.GONE
            }
        }

        // if (false) {
        //     val actionBar = getDelegate().supportActionBar
        //     actionBar?.setDisplayHomeAsUpEnabled(true)
        //     actionBar?.setDisplayShowTitleEnabled(true)
        //
        //     val menuIcon = ResourcesCompat.getDrawable(
        //         getResources(),
        //         R.drawable.ic_arrow_back,
        //         null
        //     )
        //     viewThemeUtils.androidx.themeActionBar(
        //         this,
        //         actionBar!!,
        //         title!!,
        //         menuIcon!!
        //     )
        // }

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
            super.onBackPressed()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("CoroutineCreationDuringComposition")
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

                AssistantScreen(
                    viewModel = AssistantViewModel(
                        accountName = userAccountManager.user.accountName,
                        remoteRepository = AssistantRemoteRepositoryImpl(client, capabilities),
                        localRepository = AssistantLocalRepositoryImpl(dao),
                        sessionIdArg = sessionId
                    ),
                    conversationViewModel = ConversationViewModel(
                        remoteRepository = ConversationRemoteRepositoryImpl(client)
                    ),
                    activity = this,
                    capability = capabilities
                )
            }
        } else if (destination == ComposeDestination.ClientIntegrationScreen) {
            binding.bottomNavigation.visibility = View.GONE

            val clientIntegrationUI: ClientIntegrationUI? = intent.getParcelableExtra(ARGS_CLIENT_INTEGRATION_UI)

            clientIntegrationUI?.let { ClientIntegrationScreen(it, nextcloudClient?.baseUri.toString()) }
            
        } else {
            Unit 
        }
    }
}
