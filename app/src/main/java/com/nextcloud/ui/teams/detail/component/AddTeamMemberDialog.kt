/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.nextcloud.ui.compose.component.alertDialog.BaseAlertDialog
import com.nextcloud.ui.teams.detail.model.AddTeamMemberStatus
import com.nextcloud.ui.teams.detail.model.TeamMemberSuggestion
import com.owncloud.android.R

@Composable
fun AddTeamMemberDialog(
    query: String,
    status: AddTeamMemberStatus,
    suggestions: List<TeamMemberSuggestion>,
    onQueryChange: (String) -> Unit,
    onSelect: (TeamMemberSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    val isAdding = status == AddTeamMemberStatus.Adding
    val failure = status as? AddTeamMemberStatus.Failed

    BaseAlertDialog(title = stringResource(R.string.team_add_member), onDismiss = onDismiss) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.team_add_member_hint)) },
            supportingText = failure?.let { { Text(stringResource(it.messageId)) } },
            isError = failure != null,
            readOnly = isAdding,
            singleLine = true
        )
        if (isAdding) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(modifier = Modifier.heightIn(max = dimensionResource(R.dimen.team_member_suggestions_max_height))) {
            items(suggestions, key = { "${it.member.type}:${it.member.id}" }) { suggestion ->
                ListItem(
                    headlineContent = {
                        Text(text = suggestion.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = { TeamMemberIcon(suggestion.member.type) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(enabled = !isAdding) { onSelect(suggestion) }
                )
            }
        }
    }
}
