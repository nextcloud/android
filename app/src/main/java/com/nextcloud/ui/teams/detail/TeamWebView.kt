/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.utils.IntentUtil
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.ui.activity.ExternalSiteWebView
import kotlinx.coroutines.launch

class TeamWebView : ExternalSiteWebView() {

    override fun postOnCreate() {
        super.postOnCreate()
        val pageUrl = intent.getStringExtra(EXTRA_PAGE_URL) ?: return

        webView.settings.userAgentString = null
        webView.webViewClient = object : NextcloudWebViewClient(supportFragmentManager) {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                if (request.url.hasSameOrigin(pageUrl.toUri())) {
                    return false
                }

                IntentUtil.startLinkIntent(this@TeamWebView, request.url)
                return true
            }
        }

        proxyUtil?.applyProxy { loadSignedIn(pageUrl) }
    }

    private fun loadSignedIn(pageUrl: String) {
        lifecycleScope.launch {
            val client = clientRepository.getNextcloudClient()
            if (client == null || !pageUrl.toUri().hasSameOrigin(client.baseUri)) {
                webView.loadUrl(pageUrl)
                return@launch
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.setCookies(client.baseUri.toString(), BROWSER_STATE_COOKIES) {
                    webView.loadUrl(pageUrl, mapOf(AUTHORIZATION_HEADER to client.credentials))
                }
            }
        }
    }

    private fun CookieManager.setCookies(url: String, cookies: List<String>, onDone: () -> Unit) {
        if (cookies.isEmpty()) {
            onDone()
            return
        }
        setCookie(url, cookies.first()) { setCookies(url, cookies.drop(1), onDone) }
    }

    private fun Uri.hasSameOrigin(other: Uri): Boolean =
        scheme == other.scheme && host == other.host && port == other.port

    companion object {
        private const val EXTRA_PAGE_URL = "PAGE_URL"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private val BROWSER_STATE_COOKIES = listOf(
            "cookie_test=test; path=/",
            "nc_sameSiteCookielax=true; path=/; SameSite=Lax",
            "nc_sameSiteCookiestrict=true; path=/; SameSite=Strict"
        )

        fun intent(context: Context, title: String, pageUrl: String): Intent =
            Intent(context, TeamWebView::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PAGE_URL, pageUrl)
            }
    }
}
