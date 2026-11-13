/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.alertDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.owncloud.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    onComplete: (() -> Unit)? = null,
    completeText: String = stringResource(R.string.common_ok),
    isCompleteEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // Previews cannot render the separate window opened by BasicAlertDialog, so draw the card inline there
    if (LocalInspectionMode.current) {
        BaseAlertDialogContent(title, onDismiss, onComplete, completeText, isCompleteEnabled, content)
        return
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        BaseAlertDialogContent(title, onDismiss, onComplete, completeText, isCompleteEnabled, content)
    }
}

@Composable
private fun BaseAlertDialogContent(
    title: String,
    onDismiss: () -> Unit,
    onComplete: (() -> Unit)?,
    completeText: String,
    isCompleteEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AlertDialogDefaults.titleContentColor
            )
            content()
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding))
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                onComplete?.let {
                    FilledTonalButton(onClick = it, enabled = isCompleteEnabled) {
                        Text(completeText)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BaseAlertDialogPreview() {
    MaterialTheme {
        BaseAlertDialog(
            title = "Title",
            onDismiss = {},
            onComplete = {}
        ) {
            Text(
                "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the" +
                    " industry's standard dummy text ever since 1966, when designers at Letraset and James Mosley, the"
            )
        }
    }
}
