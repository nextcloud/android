/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.ui.composeComponents.alertDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owncloud.android.R

@Suppress("LongParameterList")
@Composable
fun SimpleAlertDialog(
    title: String,
    description: String?,
    heightFraction: Float? = null,
    content: @Composable (() -> Unit)? = null,
    onComplete: () -> Unit,
    dismiss: () -> Unit
) {
    val modifier = if (heightFraction != null) {
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
    } else {
        Modifier.fillMaxWidth()
    }

    AlertDialog(
        onDismissRequest = { dismiss() },
        title = {
            Text(text = title)
        },
        text = {
            Column(modifier = modifier) {
                description?.let {
                    Text(text = description)
                }

                content?.let {
                    Spacer(modifier = Modifier.height(16.dp))

                    content()
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                onComplete()
                dismiss()
            }) {
                Text(
                    stringResource(id = R.string.common_ok)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { dismiss() }) {
                Text(
                    stringResource(id = R.string.common_cancel)
                )
            }
        }
    )
}
