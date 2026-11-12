/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.nextcloud.ui.teams.detail.model.TeamDetailListUiState
import com.owncloud.android.R

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
            is TeamDetailListUiState.Error -> ErrorMessage(state.messageId, onRetry)
            is TeamDetailListUiState.Content -> Message(emptyMessageId)
        }
    }
}

@Composable
private fun ErrorMessage(@StringRes messageId: Int, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding))
    ) {
        Message(messageId)
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun Message(@StringRes messageId: Int) {
    Text(
        text = stringResource(messageId),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
