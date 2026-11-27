/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nextcloud.ui.teams.component.TeamsErrorMessage
import com.nextcloud.ui.teams.component.TeamsMessage
import com.nextcloud.ui.teams.detail.model.TeamDetailListUiState

@Composable
fun <T> TeamDetailListStateContent(
    state: TeamDetailListUiState<T>,
    @StringRes emptyMessageId: Int,
    onRetry: () -> Unit,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit
) {
    if (state is TeamDetailListUiState.Content && state.items.isNotEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.items, key = key) { itemContent(it) }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            TeamDetailListUiState.Loading -> CircularProgressIndicator()
            is TeamDetailListUiState.Error -> TeamsErrorMessage(state.messageId, onRetry)
            is TeamDetailListUiState.Content -> TeamsMessage(emptyMessageId)
        }
    }
}
