/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nextcloud.ui.teams.model.CreateTeamStatus
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.owncloud.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamDialog(
    name: String,
    status: CreateTeamStatus,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        CreateTeamDialogCard(name, status, onNameChange, onDismiss, onCreate)
    }
}

@Composable
private fun CreateTeamDialogCard(
    name: String,
    status: CreateTeamStatus,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    val isCreating = status == CreateTeamStatus.Creating
    val isNameValid = TeamsViewModel.isValidTeamName(name)

    Surface(
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.dialog_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_padding))
        ) {
            Text(
                text = stringResource(R.string.teams_create),
                style = MaterialTheme.typography.headlineSmall,
                color = AlertDialogDefaults.titleContentColor
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.teams_name_hint)) },
                supportingText = { CreateTeamSupportingText(name, status, isNameValid) },
                isError = status is CreateTeamStatus.Failed,
                readOnly = isCreating,
                singleLine = true
            )
            if (isCreating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding))
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                TextButton(onClick = onCreate, enabled = isNameValid && !isCreating) {
                    Text(stringResource(R.string.create))
                }
            }
        }
    }
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
        CreateTeamDialogCard(name = name, status = status, onNameChange = {}, onDismiss = {}, onCreate = {})
    }
}

@Preview
@Composable
private fun CreateTeamDialogEditingPreview() {
    CreateTeamDialogPreview(name = "Editing", status = CreateTeamStatus.Editing)
}

@Preview
@Composable
private fun CreateTeamDialogCreatingPreview() {
    CreateTeamDialogPreview(name = "Creating", status = CreateTeamStatus.Creating)
}

@Preview
@Composable
private fun CreateTeamDialogFailedPreview() {
    CreateTeamDialogPreview(
        name = "Failed",
        status = CreateTeamStatus.Failed(R.string.teams_error_create_not_allowed)
    )
}
