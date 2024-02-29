/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.ui.composeActivity

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.client.assistant.AssistantViewModel
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

class ComposeActivity : DrawerActivity() {

    lateinit var binding: ActivityComposeBinding

    companion object {
        const val destinationKey = "destinationKey"
        const val titleKey = "titleKey"
        const val menuItemKey = "menuItemKey"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = intent.getSerializableArgument(destinationKey, ComposeDestination::class.java)
        val titleId = intent.getIntExtra(titleKey, R.string.empty)
        val menuItemId = intent.getIntExtra(menuItemKey, R.id.nav_assistant)

        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(titleId))

        setupDrawer(menuItemId)

        binding.composeView.setContent {
            Content(destination, storageManager.user, this)
        }
    }

    override fun onResume() {
        super.onResume()
        setDrawerMenuItemChecked(R.id.nav_assistant)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        if (item.itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else {
                openDrawer()
            }
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    @Composable
    private fun Content(destination: ComposeDestination?, user: User, context: Context) {
        var nextcloudClient by remember { mutableStateOf<NextcloudClient?>(null) }

        LaunchedEffect(Unit) {
            nextcloudClient = getNextcloudClient(user, context)
        }

        if (destination == ComposeDestination.AssistantScreen) {
            nextcloudClient?.let {
                AssistantScreen(
                    viewModel = AssistantViewModel(
                        client = it
                    )
                )
            }
        }
    }

    private suspend fun getNextcloudClient(user: User, context: Context): NextcloudClient? {
        return withContext(Dispatchers.IO) {
            try {
                OwnCloudClientFactory.createNextcloudClient(user, context)
            } catch (e: AccountUtils.AccountNotFoundException) {
                Log_OC.e(this, "Error caught at init of AssistantRepository", e)
                null
            }
        }
    }
}
