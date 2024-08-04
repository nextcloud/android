/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.composeActivity

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.client.assistant.repository.AssistantRepository
import com.nextcloud.common.NextcloudClient
import com.nextcloud.common.User
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityComposeBinding
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.DrawerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class ComposeActivity : DrawerActivity() {

    lateinit var binding: ActivityComposeBinding
    private var menuItemId: Int = R.id.nav_all_files

    companion object {
        const val DESTINATION = "DESTINATION"
        const val TITLE = "TITLE"
        const val MENU_ITEM = "MENU_ITEM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = intent.getSerializableArgument(DESTINATION, ComposeDestination::class.java)
        val titleId = intent.getIntExtra(TITLE, R.string.empty)
        menuItemId = intent.getIntExtra(MENU_ITEM, R.id.nav_all_files)

        setupDrawer(menuItemId)

        setupToolbarShowOnlyMenuButtonAndTitle(getString(titleId)) {
            openDrawer()
        }

        binding.composeView.setContent {
            MaterialTheme(
                colorScheme = viewThemeUtils.getColorScheme(this),
                content = {
                    Content(destination, storageManager.user, this)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setDrawerMenuItemChecked(menuItemId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                toggleDrawer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Composable
    private fun Content(destination: ComposeDestination?, user: User, context: Context) {
        var nextcloudClient by remember { mutableStateOf<NextcloudClient?>(null) }

        LaunchedEffect(Unit) {
            nextcloudClient = getNextcloudClient(user, context)
        }

        if (destination == ComposeDestination.AssistantScreen) {
            nextcloudClient?.let { client ->
                AssistantScreen(
                    viewModel = AssistantViewModel(
                        repository = AssistantRepository(client),
                        context = WeakReference(this)
                    ),
                    activity = this
                )
            }
        }
    }

    private suspend fun getNextcloudClient(user: User, context: Context): NextcloudClient? {
        return withContext(Dispatchers.IO) {
            try {
                OwnCloudClientFactory.createNextcloudClient(user, context)
            } catch (e: AccountUtils.AccountNotFoundException) {
                Log_OC.e(this, "Error caught at init of createNextcloudClient", e)
                null
            }
        }
    }
}
