/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nextcloud.android.lib.resources.clientintegration.Button
import com.nextcloud.android.lib.resources.clientintegration.ClientIntegrationUI
import com.nextcloud.android.lib.resources.clientintegration.Element
import com.nextcloud.android.lib.resources.clientintegration.Layout
import com.nextcloud.android.lib.resources.clientintegration.Orientation
import com.nextcloud.android.lib.resources.clientintegration.Text
import com.nextcloud.android.lib.resources.clientintegration.URL
import com.nextcloud.utils.extensions.getActivity
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.DisplayUtils

@Composable
fun ClientIntegrationScreen(clientIntegrationUI: ClientIntegrationUI, baseUrl: String) {
    val activity = LocalContext.current.getActivity()

    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { activity?.finish() }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close"
                )
            }
        }
    }, modifier = Modifier.fillMaxSize()) {
        when (clientIntegrationUI.root.orientation) {
            Orientation.VERTICAL -> {
                LazyColumn {
                    items(clientIntegrationUI.root.rows) { row ->
                        LazyRow {
                            items(row.children) { element ->
                                DisplayElement(element, baseUrl, activity)
                            }
                        }
                    }
                }
            }
            else -> {
                LazyRow {
                    items(clientIntegrationUI.root.rows) { row ->
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
        is Button -> androidx.compose.material3.Button(onClick = { }) {
            androidx.compose.material3.Text(element.label)
        }

        is URL -> TextButton({
            openLink(activity, baseUrl, element.url)
        }) { androidx.compose.material3.Text(element.text) }

        is Text -> androidx.compose.material3.Text(element.text)
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
            Orientation.VERTICAL,
            mutableListOf(
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click", "Primary"), Text("123"))
                ),
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click2", "Primary"))
                ),
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(URL("Analytics report created", "https://nextcloud.com"))
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
            Orientation.HORIZONTAL,
            mutableListOf(
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click", "Primary"), Text("123"))
                ),
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click2", "Primary"))
                ),
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(URL("Analytics report created", "https://nextcloud.com"))
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
            Orientation.HORIZONTAL,
            emptyList()
        )
    )

    ClientIntegrationScreen(clientIntegrationUI, "http://nextcloud.local")
}
