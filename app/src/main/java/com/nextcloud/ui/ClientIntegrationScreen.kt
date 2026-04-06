/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2025 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nextcloud.android.lib.resources.clientintegration.ClientIntegrationUI
import com.nextcloud.android.lib.resources.clientintegration.Element
import com.nextcloud.android.lib.resources.clientintegration.Layout
import com.nextcloud.android.lib.resources.clientintegration.LayoutButton
import com.nextcloud.android.lib.resources.clientintegration.LayoutOrientation
import com.nextcloud.android.lib.resources.clientintegration.LayoutRow
import com.nextcloud.android.lib.resources.clientintegration.LayoutText
import com.nextcloud.android.lib.resources.clientintegration.LayoutURL
import com.nextcloud.utils.extensions.getActivity
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.DisplayUtils

@Composable
fun ClientIntegrationScreen(clientIntegrationUI: ClientIntegrationUI, baseUrl: String) {
    val activity = LocalContext.current.getActivity()
    val layoutRows = clientIntegrationUI.root?.rows ?: listOf()

    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { activity?.finish() }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.dialog_close)
                )
            }
        }
    }, modifier = Modifier.fillMaxSize()) {
        when (clientIntegrationUI.root?.orientation) {
            LayoutOrientation.VERTICAL -> {
                LazyColumn(modifier = Modifier.padding(it)) {
                    items(layoutRows) { row ->
                        LazyRow {
                            items(row.children) { element ->
                                DisplayElement(element, baseUrl, activity)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyRow(modifier = Modifier.padding(it)) {
                    items(layoutRows) { row ->
                        LazyColumn {
                            items(row.children) { element ->
                                DisplayElement(element, baseUrl, activity)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayElement(element: Element, baseUrl: String, activity: Activity?) {
    when (element) {
        is LayoutButton -> Button(onClick = { }) {
            Text(element.label)
        }

        is LayoutURL -> TextButton({
            openLink(activity, baseUrl, element.url)
        }) { Text(element.text) }

        is LayoutText -> Text(element.text)
    }
}

private fun openLink(activity: Activity?, baseUrl: String, relativeUrl: String) {
    activity?.let {
        DisplayUtils.startLinkIntent(activity, baseUrl + relativeUrl)
    }
}

@Composable
@Preview
private fun ClientIntegrationScreenPreviewVertical() {
    val clientIntegrationUI = ClientIntegrationUI(
        OCCapability.CLIENT_INTEGRATION_VERSION,
        Layout(
            LayoutOrientation.VERTICAL,
            mutableListOf(
                LayoutRow(
                    listOf(LayoutButton("Click", "Primary"), LayoutText("123"))
                ),
                LayoutRow(
                    listOf(LayoutButton("Click2", "Primary"))
                ),
                LayoutRow(
                    listOf(LayoutURL("Analytics report created", "https://nextcloud.com"))
                )
            )
        )
    )

    ClientIntegrationScreen(
        clientIntegrationUI,
        "http://nextcloud.local"
    )
}

@Composable
@Preview
private fun ClientIntegrationScreenPreviewHorizontal() {
    val clientIntegrationUI = ClientIntegrationUI(
        OCCapability.CLIENT_INTEGRATION_VERSION,
        Layout(
            LayoutOrientation.HORIZONTAL,
            mutableListOf(
                LayoutRow(
                    listOf(LayoutButton("Click", "Primary"), LayoutText("123"))
                ),
                LayoutRow(
                    listOf(LayoutButton("Click2", "Primary"))
                ),
                LayoutRow(
                    listOf(LayoutURL("Analytics report created", "https://nextcloud.com"))
                )
            )
        )
    )

    ClientIntegrationScreen(clientIntegrationUI, "http://nextcloud.local")
}

@Composable
@Preview
private fun ClientIntegrationScreenPreviewEmpty() {
    val clientIntegrationUI = ClientIntegrationUI(
        OCCapability.CLIENT_INTEGRATION_VERSION,
        Layout(
            LayoutOrientation.HORIZONTAL,
            emptyList()
        )
    )

    ClientIntegrationScreen(clientIntegrationUI, "http://nextcloud.local")
}
