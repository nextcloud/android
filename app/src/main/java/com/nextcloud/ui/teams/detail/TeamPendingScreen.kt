/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nextcloud.ui.teams.component.TeamsMessage
import com.nextcloud.ui.teams.detail.component.TeamDetailTopBar
import com.owncloud.android.R

@Composable
fun TeamPendingScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TeamDetailTopBar(title, onBack) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            TeamsMessage(R.string.team_pending_approval)
        }
    }
}
