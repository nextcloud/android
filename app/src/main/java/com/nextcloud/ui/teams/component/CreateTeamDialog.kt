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
import com.nextcloud.ui.teams.model.TeamDialogStatus
import com.nextcloud.ui.teams.viewmodel.TeamsViewModel
import com.owncloud.android.R

@Composable
fun CreateTeamDialog(
    name: String,
    status: TeamDialogStatus,
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

private fun canCreate(name: String, status: TeamDialogStatus): Boolean =
    TeamsViewModel.isValidTeamName(name) && status != TeamDialogStatus.Submitting

@Composable
private fun CreateTeamDialogFields(name: String, status: TeamDialogStatus, onNameChange: (String) -> Unit) {
    val isCreating = status == TeamDialogStatus.Submitting

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.teams_name_hint)) },
        supportingText = { CreateTeamSupportingText(name, status) },
        isError = status is TeamDialogStatus.Failed,
        readOnly = isCreating,
        singleLine = true
    )
    if (isCreating) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CreateTeamSupportingText(name: String, status: TeamDialogStatus) {
    if (status is TeamDialogStatus.Failed) {
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
private fun CreateTeamDialogPreview(name: String, status: TeamDialogStatus) {
    MaterialTheme {
        CreateTeamDialog(name = name, status = status, onNameChange = {}, onDismiss = {}, onCreate = {})
    }
}

@Preview
@Composable
private fun CreateTeamDialogEditingPreview() {
    CreateTeamDialogPreview(name = "Editing", status = TeamDialogStatus.Editing)
}

@Preview
@Composable
private fun CreateTeamDialogCreatingPreview() {
    CreateTeamDialogPreview(name = "Creating", status = TeamDialogStatus.Submitting)
}

@Preview
@Composable
private fun CreateTeamDialogFailedPreview() {
    CreateTeamDialogPreview(
        name = "Failed",
        status = TeamDialogStatus.Failed(R.string.teams_error_create_not_allowed)
    )
}
