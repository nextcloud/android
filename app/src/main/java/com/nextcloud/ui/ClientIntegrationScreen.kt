/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.owncloud.android.utils.DisplayUtils

@Composable
fun ClientIntegrationScreen(clientIntegrationUI: ClientIntegrationUI, baseUrl: String) {
    val activity = LocalContext.current.getActivity()


    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton({ close(activity) }) {
                androidx.compose.material3.Text("X")
            }
        }

        Row {
            if (clientIntegrationUI.root.orientation == Orientation.VERTICAL) {
                Column {
                    clientIntegrationUI.root.rows.forEach { row ->
                        Row {
                            row.children.forEach { element ->
                                DisplayElement(element, baseUrl, activity)
                            }
                        }
                    }
                }
            } else {
                Row {
                    clientIntegrationUI.root.rows.forEach { row ->
                        Column {
                            row.children.forEach { element ->
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
        is Button -> androidx.compose.material3.Button({ }) {
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

private fun close(activity: Activity?) {
    activity?.finish()
}

@Composable
@Preview
private fun ClientIntegrationScreenPreviewVertical() {
    val clientIntegrationUI = ClientIntegrationUI(
        0.1, Layout(
            Orientation.VERTICAL,
            mutableListOf(
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click", "Primary"), Text("123"))
                ), com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click2", "Primary"))
                ), com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(URL("Analytics report created", "https://nextcloud.com"))
                )
            )
        )
    )

    ClientIntegrationScreen(
        clientIntegrationUI, "http://nextcloud.local"
    )
}

@Composable
@Preview
private fun ClientIntegrationScreenPreviewHorizontal() {
    val clientIntegrationUI = ClientIntegrationUI(
        0.1, Layout(
            Orientation.HORIZONTAL, mutableListOf(
                com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click", "Primary"), Text("123"))
                ), com.nextcloud.android.lib.resources.clientintegration.Row(
                    listOf(Button("Click2", "Primary"))
                ), com.nextcloud.android.lib.resources.clientintegration.Row(
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
        0.1, Layout(
            Orientation.HORIZONTAL,
            emptyList()
        )
    )

    ClientIntegrationScreen(clientIntegrationUI, "http://nextcloud.local")
}
