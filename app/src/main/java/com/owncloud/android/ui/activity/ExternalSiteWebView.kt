/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.utils.RawResourceReader
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.ExternalsiteWebviewBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.utils.WebViewUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * This activity shows an URL as a web view
 */
open class ExternalSiteWebView : FileActivity() {
    private lateinit var binding: ExternalsiteWebviewBinding

    private var showToolbar = true
    private var showSidebar = false
    protected var url: String? = null

    private val isDebuggable: Boolean
        get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    final override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")

        if (!WebViewUtil.available(this)) {
            super.onCreate(savedInstanceState)
            SnackbarUtil.show(this, R.string.webview_not_available)
            finish()
            return
        }

        bindView()
        readIntentExtras()
        window?.requestFeature(Window.FEATURE_PROGRESS)

        super.onCreate(savedInstanceState)
        setContentView(rootView)
        postOnCreate()
    }

    private fun readIntentExtras() {
        url = intent.getStringExtra(EXTRA_URL)
        showToolbar = intent.getBooleanExtra(EXTRA_SHOW_TOOLBAR, showToolbarByDefault())
        showSidebar = intent.getBooleanExtra(EXTRA_SHOW_SIDEBAR, false)
    }

    protected open fun postOnCreate() {
        webView.run {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
        }

        // allow debugging (when building the debug version); see details in
        // https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if (isDebuggable || resources.getBoolean(R.bool.is_beta)) {
            Log_OC.d(this, "Enable debug for webView")
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setupToolbarAndDrawer()
        setupWebSettings(webView.settings)
        webView.webViewClient = createWebViewClient()

        WebViewUtil().setProxyKKPlus(webView)
        url?.let { webView.loadUrl(it) }
    }

    private fun setupToolbarAndDrawer() {
        if (showToolbar) {
            setupToolbar()
        } else {
            findViewById<View?>(R.id.appbar)?.isVisible = false
        }

        setupDrawer(R.id.nav_view)

        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        val title = intent.getStringExtra(EXTRA_TITLE)
        if (!title.isNullOrEmpty()) {
            setupActionBar(title)
        }
    }

    private fun createWebViewClient() = object : NextcloudWebViewClient(supportFragmentManager) {
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            val customError = RawResourceReader.readText(resources, R.raw.custom_error)
            if (customError.isNotEmpty()) {
                webView.loadData(customError, CUSTOM_ERROR_MIME_TYPE, null)
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
            if (request.isRedirect) {
                return false
            }

            IntentUtil.startLinkIntent(this@ExternalSiteWebView, request.url)
            return true
        }
    }

    override fun onDestroy() {
        if (isWebViewBound) {
            webView.destroy()
        }
        super.onDestroy()
    }

    protected open val isWebViewBound: Boolean
        get() = ::binding.isInitialized

    protected open fun bindView() {
        binding = ExternalsiteWebviewBinding.inflate(layoutInflater)
    }

    protected open fun showToolbarByDefault(): Boolean = true

    protected open val rootView: View
        get() = binding.root

    protected open val webView: WebView
        get() = binding.webView

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebSettings(webSettings: WebSettings) {
        webSettings.run {
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Non-responsive webs are zoomed out when loaded
            useWideViewPort = true
            loadWithOverviewMode = true

            userAgentString = MainApp.getUserAgent()
            saveFormData = false
            allowFileAccess = false
            javaScriptEnabled = true
            domStorageEnabled = true

            if (isDebuggable) {
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
        }
    }

    private fun setupActionBar(title: String) {
        val actionBar = supportActionBar ?: return
        viewThemeUtils.files.themeActionBar(this, actionBar, title)

        if (showSidebar) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        } else {
            setDrawerIndicatorEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != android.R.id.home) {
            return super.onOptionsItemSelected(item)
        }

        when {
            !showSidebar -> finish()
            isDrawerOpen -> closeDrawer()
            else -> openDrawer()
        }
        return true
    }

    companion object {
        const val EXTRA_TITLE = "TITLE"
        const val EXTRA_URL = "URL"
        const val EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR"
        const val EXTRA_SHOW_TOOLBAR = "SHOW_TOOLBAR"
        const val EXTRA_TEMPLATE = "TEMPLATE"

        private const val CUSTOM_ERROR_MIME_TYPE = "text/html; charset=UTF-8"
        private val TAG = ExternalSiteWebView::class.java.simpleName
    }
}
