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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.nextcloud.android.lib.resources.declarativeui.GetDeclarativeUiJsonOperation
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.client.assistant.repository.AssistantRepository
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.DeclarativeUiScreen
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityComposeBinding
import com.owncloud.android.lib.resources.declarativeui.Endpoint
import com.owncloud.android.ui.activity.DrawerActivity
import kotlinx.coroutines.launch

class ComposeActivity : DrawerActivity() {

    lateinit var binding: ActivityComposeBinding

    companion object {
        const val DESTINATION = "DESTINATION"
        const val TITLE = "TITLE"
        const val ARGS_ENDPOINT = "ARGS_ENDPOINT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = intent.getSerializableArgument(DESTINATION, ComposeDestination::class.java)
        val titleId = intent.getIntExtra(TITLE, R.string.empty)

        setupDrawer()

        setupToolbarShowOnlyMenuButtonAndTitle(getString(titleId)) {
            openDrawer()
        }

        binding.composeView.setContent {
            MaterialTheme(
                colorScheme = viewThemeUtils.getColorScheme(this),
                content = {
                    Content(destination, intent)
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

    @SuppressLint("CoroutineCreationDuringComposition")
    @Composable
    private fun Content(destination: ComposeDestination?, intent1: Intent) {
        var nextcloudClient by remember { mutableStateOf<NextcloudClient?>(null) }

        LaunchedEffect(Unit) {
            nextcloudClient = clientRepository.getNextcloudClient()
        }

        if (destination == ComposeDestination.AssistantScreen) {
            binding.bottomNavigation.menu.findItem(R.id.nav_assistant).run {
                isChecked = true
            }

            nextcloudClient?.let { client ->
                AssistantScreen(
                    viewModel = AssistantViewModel(
                        repository = AssistantRepository(client, capabilities)
                    ),
                    activity = this,
                    capability = capabilities
                )
            }
        } else if (destination == ComposeDestination.DeclarativeUi) {
            binding.bottomNavigation.visibility = View.GONE

            val endpoint : Endpoint? = intent.getParcelableExtra(ARGS_ENDPOINT)

            if (nextcloudClient != null && endpoint != null) {
                val string = """{
  "Button": {
    "label": "Submit",
    "type": "primary",
  },
  "Image": {
    "url": "/core/img/logo/logo.png"
  }
}"""
                  
                        DeclarativeUiScreen(Gson().fromJson(string))
                    }
                }
                   
                //}
                
            }
        }
    }
}
