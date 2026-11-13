/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nextcloud.ui.compose.component.alertDialog.BaseAlertDialog
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
    BaseAlertDialog(
        title = stringResource(R.string.teams_create),
        onDismiss = onDismiss,
        onComplete = onCreate,
        completeText = stringResource(R.string.create),
        isCompleteEnabled = canCreate(name, status)
    ) {
        CreateTeamDialogFields(name, status, onNameChange)
    }
}

private fun canCreate(name: String, status: CreateTeamStatus): Boolean =
    TeamsViewModel.isValidTeamName(name) && status != CreateTeamStatus.Creating

@Composable
private fun CreateTeamDialogFields(name: String, status: CreateTeamStatus, onNameChange: (String) -> Unit) {
    val isCreating = status == CreateTeamStatus.Creating

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.teams_name_hint)) },
        supportingText = { CreateTeamSupportingText(name, status) },
        isError = status is CreateTeamStatus.Failed,
        readOnly = isCreating,
        singleLine = true
    )
    if (isCreating) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CreateTeamSupportingText(name: String, status: CreateTeamStatus) {
    if (status is CreateTeamStatus.Failed) {
        Text(stringResource(status.messageId))
        return
    }

    if (name.isNotBlank() && !TeamsViewModel.isValidTeamName(name)) {
        Text(
            pluralStringResource(
                R.plurals.teams_name_length,
                TeamsViewModel.MAX_TEAM_NAME_BYTES,
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
