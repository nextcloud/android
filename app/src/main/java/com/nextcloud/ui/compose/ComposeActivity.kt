/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.compose

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
import androidx.core.view.isVisible
import com.nextcloud.client.assistant.AssistantDestination
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.compose.navigation.ComposeDestination
import com.nextcloud.ui.compose.navigation.ComposeNavHost
import com.nextcloud.ui.compose.navigation.rememberComposeNavigator
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
        get() = if (destination is ComposeDestination.TeamsScreen) Menu.NONE else R.id.nav_assistant

    override fun onResume() {
        super.onResume()
        highlightNavigationViewItem(menuItemId)
    }

    private fun setupActivityUIFor(destination: ComposeDestination) {
        when (destination) {
            is ComposeDestination.AssistantScreen -> setupDrawerScreen(destination.title)
            is ComposeDestination.TeamsScreen -> setupDrawerScreen(destination.title)
            else -> setSupportActionBar(null)
        }
    }

    private fun setupDrawerScreen(title: String) {
        setupDrawer(menuItemId)
        showMenuAndTitleActionBar(title) {
            openDrawer()
        }
    }

    private fun showActivityBarsFor(screen: ComposeDestination) {
        val isDrawerScreen = screen is ComposeDestination.AssistantScreen || screen is ComposeDestination.TeamsScreen
        findViewById<View?>(R.id.appbar)?.isVisible = isDrawerScreen
        binding.bottomNavigation.isVisible = isDrawerScreen

        if (screen is ComposeDestination.AssistantScreen) {
            binding.bottomNavigation.menu.findItem(R.id.nav_assistant)?.isChecked = true
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
        val navigator = rememberComposeNavigator(destination, onExit = ::finish)

        LaunchedEffect(Unit) {
            nextcloudClient = clientRepository.getNextcloudClient()
        }

        LaunchedEffect(navigator.current) {
            showActivityBarsFor(navigator.current)
        }

        ComposeNavHost(
            navigator = navigator,
            vmFactory = vmFactory,
            user = userAccountManager.user,
            nextcloudClient = nextcloudClient
        ) { screen ->
            AssistantDestination(
                screen = screen,
                composeViewModel = composeViewModel,
                accountName = userAccountManager.user.accountName,
                nextcloudClient = nextcloudClient,
                capability = capabilities.orElse(null)
            )
        }
    }
}
