/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.webView

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R

private const val MAX_PROGRESS = 100
private const val AUTHORIZATION_HEADER = "Authorization"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageScreen(title: String, url: String, client: NextcloudClient, onClose: () -> Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.common_back))
                    }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (progress < MAX_PROGRESS) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / MAX_PROGRESS },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        configure(
                            onProgressChange = { progress = it },
                            onHistoryChange = { canGoBack = it },
                            onRendererGone = onClose
                        )
                        loadAuthenticated(url, client)
                        webView = this
                    }
                },
                onRelease = { it.destroy() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configure(
    onProgressChange: (Int) -> Unit,
    onHistoryChange: (Boolean) -> Unit,
    onRendererGone: () -> Unit
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
    }

    webViewClient = object : WebViewClient() {
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            onHistoryChange(view.canGoBack())
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            onRendererGone()
            return true
        }
    }
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            onProgressChange(newProgress)
        }
    }
}

private fun WebView.loadAuthenticated(url: String, client: NextcloudClient) {
    if (!url.toUri().hasSameOrigin(client.baseUri)) {
        loadUrl(url)
        return
    }

    CookieManager.getInstance().removeAllCookies {
        loadUrl(url, mapOf(AUTHORIZATION_HEADER to client.credentials))
    }
}

private fun Uri.hasSameOrigin(other: Uri): Boolean = scheme == other.scheme && host == other.host && port == other.port
