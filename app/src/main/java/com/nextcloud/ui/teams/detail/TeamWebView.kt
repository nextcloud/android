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
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.common.OkHttpMethodBase
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.ui.activity.ExternalSiteWebView
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class TeamWebView : ExternalSiteWebView() {

    override fun postOnCreate() {
        super.postOnCreate()
        val pageUrl = intent.getStringExtra(EXTRA_PAGE_URL) ?: return

        webView.settings.userAgentString = null
        webView.webViewClient = object : NextcloudWebViewClient(supportFragmentManager) {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                if (request.url.toString().hasSameOrigin(pageUrl)) {
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
            if (client == null || !pageUrl.hasSameOrigin(client.baseUri.toString())) {
                webView.loadUrl(pageUrl)
                return@launch
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                val cookies = listOf(BROWSER_SESSION_COOKIE) + sameSiteCookies(client.baseUri)
                cookieManager.setCookies(client.baseUri.toString(), cookies) {
                    webView.loadUrl(pageUrl, mapOf(OkHttpMethodBase.AUTHORIZATION to client.credentials))
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

    private fun sameSiteCookies(baseUri: Uri): List<String> {
        val isInstalledAtRoot = baseUri.path.orEmpty().trimEnd(PATH_SEPARATOR).isEmpty()
        if (!baseUri.scheme.equals(HTTPS_SCHEME, ignoreCase = true) || !isInstalledAtRoot) {
            return SAME_SITE_COOKIES
        }

        return SAME_SITE_COOKIES + SAME_SITE_COOKIES.map { "$HOST_COOKIE_PREFIX$it; Secure" }
    }

    private fun String.hasSameOrigin(other: String): Boolean {
        val url = toHttpUrlOrNull() ?: return false
        val otherUrl = other.toHttpUrlOrNull() ?: return false

        return url.scheme == otherUrl.scheme && url.host == otherUrl.host && url.port == otherUrl.port
    }

    companion object {
        private const val EXTRA_PAGE_URL = "PAGE_URL"
        private const val HTTPS_SCHEME = "https"
        private const val HOST_COOKIE_PREFIX = "__Host-"
        private const val PATH_SEPARATOR = '/'
        private const val BROWSER_SESSION_COOKIE = "cookie_test=test; path=/"
        private val SAME_SITE_COOKIES = listOf(
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
