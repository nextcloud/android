/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nextcloud.ui.teams.model.CreateTeamStatus
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.owncloud.android.R

@Composable
fun CreateTeamDialog(
    name: String,
    status: CreateTeamStatus,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    val isCreating = status == CreateTeamStatus.Creating
    val isNameValid = TeamsViewModel.isValidTeamName(name)

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(stringResource(R.string.teams_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding))) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.teams_name_hint)) },
                    supportingText = { CreateTeamSupportingText(name, status, isNameValid) },
                    isError = status is CreateTeamStatus.Failed,
                    enabled = !isCreating,
                    singleLine = true
                )
                if (isCreating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate, enabled = isNameValid && !isCreating) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun CreateTeamSupportingText(name: String, status: CreateTeamStatus, isNameValid: Boolean) {
    if (status is CreateTeamStatus.Failed) {
        Text(stringResource(status.messageId))
        return
    }

    if (name.isNotBlank() && !isNameValid) {
        Text(
            stringResource(
                R.string.teams_name_length,
                TeamsViewModel.MIN_TEAM_NAME_BYTES,
                TeamsViewModel.MAX_TEAM_NAME_BYTES
            )
        )
    }
}

@Composable
private fun CreateTeamDialogPreview(name: String, status: CreateTeamStatus) {
    MaterialTheme {
        CreateTeamDialog(name = name, status = status, onNameChange = {}, onDismiss = {}, onCreate = {})
    }
}

@Preview
@Composable
private fun CreateTeamDialogEditingPreview() {
    CreateTeamDialogPreview(name = "Design team", status = CreateTeamStatus.Editing)
}

@Preview
@Composable
private fun CreateTeamDialogCreatingPreview() {
    CreateTeamDialogPreview(name = "Design team", status = CreateTeamStatus.Creating)
}

@Preview
@Composable
private fun CreateTeamDialogFailedPreview() {
    CreateTeamDialogPreview(
        name = "Design team",
        status = CreateTeamStatus.Failed(R.string.teams_error_create_not_allowed)
    )
}
