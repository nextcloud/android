/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.compose.navigation

import android.content.Context
import android.os.Parcelable
import com.nextcloud.android.lib.resources.clientintegration.ClientIntegrationUI
import com.owncloud.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ComposeDestination : Parcelable {
    @Parcelize
    data class AssistantScreen(val title: String, val sessionId: Long?) : ComposeDestination()

    @Parcelize
    data class ClientIntegrationScreen(val title: String, val data: ClientIntegrationUI) : ComposeDestination()

    @Parcelize
    data class TeamsScreen(val title: String) : ComposeDestination()

    @Parcelize
    data class TeamDetailScreen(val teamId: String, val title: String, val settingsUrl: String) : ComposeDestination()

    companion object {
        fun getTeamsScreen(context: Context): TeamsScreen = TeamsScreen(context.getString(R.string.navigation_teams))

        /**
         * Creates a assistant screen without selected chat
         */
        fun getAssistantScreen(context: Context): AssistantScreen =
            AssistantScreen(context.getString(R.string.assistant_screen_top_bar_title), null)
    }
}
