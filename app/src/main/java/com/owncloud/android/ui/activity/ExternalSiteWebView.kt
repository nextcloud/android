/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.drawerlayout.widget.DrawerLayout
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.ExternalsiteWebviewBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.utils.DisplayUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * This activity shows an URL as a web view
 */
open class ExternalSiteWebView : FileActivity() {

    private var showToolbar = true
    private lateinit var binding: ExternalsiteWebviewBinding
    private var menuItemId = 0
    private var showSidebar = false

    @JvmField
    var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")
        bindView()
        showToolbar = showToolbarByDefault()
        getArguments()
        showProgress()
        super.onCreate(savedInstanceState)
        setContentView(rootView)
        postOnCreate()
    }

    private fun getArguments() {
        val extras = intent.extras
        url = intent.extras?.getString(EXTRA_URL)
        if (extras?.containsKey(EXTRA_SHOW_TOOLBAR) == true) {
            showToolbar = extras.getBoolean(EXTRA_SHOW_TOOLBAR)
        }

        menuItemId = extras?.getInt(EXTRA_MENU_ITEM_ID) ?: 0
        showSidebar = extras?.getBoolean(EXTRA_SHOW_SIDEBAR) ?: false
    }

    @Suppress("DEPRECATION")
    private fun showProgress() {
        val window = window
        window?.requestFeature(Window.FEATURE_PROGRESS)
    }

    protected open fun postOnCreate() {
        val webSettings = webView.settings
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.isClickable = true

        // allow debugging (when building the debug version); see details in
        // https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 ||
            resources.getBoolean(R.bool.is_beta)
        ) {
            Log_OC.d(this, "Enable debug for webView")
            WebView.setWebContentsDebuggingEnabled(true)
        }

        initToolbar()

        setupDrawer(menuItemId)
        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        val title = intent.extras!!.getString(EXTRA_TITLE)
        if (!TextUtils.isEmpty(title)) {
            setupActionBar(title)
        }

        setupWebSettings(webSettings)
        setupProgressBar()
        setupWebView()
    }

    private fun initToolbar() {
        if (showToolbar) {
            setupToolbar()
        } else {
            if (findViewById<View?>(R.id.appbar) != null) {
                findViewById<View>(R.id.appbar).visibility = View.GONE
            }
        }
    }

    private fun setupProgressBar() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        if (progressBar != null) {
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    progressBar.progress = progress * 1000
                }
            }
        }
    }

    private fun setupWebView() {
        val self = this
        webView.webViewClient = object : NextcloudWebViewClient(supportFragmentManager) {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                val resources = resources.openRawResource(R.raw.custom_error)
                val customError = DisplayUtils.getData(resources)
                if (customError.isNotEmpty()) {
                    webView.loadData(customError, "text/html; charset=UTF-8", null)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                DisplayUtils.startLinkIntent(self, request.url)
                return true
            }
        }
        url?.let {
            webView.loadUrl(it)
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    protected open fun bindView() {
        binding = ExternalsiteWebviewBinding.inflate(layoutInflater)
    }

    protected open fun showToolbarByDefault(): Boolean {
        return true
    }

    protected open val rootView: View?
        get() = binding.root

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebSettings(webSettings: WebSettings) {
        // enable zoom
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        // Non-responsive webs are zoomed out when loaded
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        // user agent
        webSettings.userAgentString = MainApp.getUserAgent()

        // do not store private data
        webSettings.saveFormData = false

        // disable local file access
        webSettings.allowFileAccess = false

        // enable javascript
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // caching disabled in debug mode
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        }
    }

    private fun setupActionBar(title: String?) {
        supportActionBar?.let {
            title?.let { title ->
                viewThemeUtils.files.themeActionBar(this, it, title)
            }

            if (showSidebar) {
                it.setDisplayHomeAsUpEnabled(true)
            } else {
                setDrawerIndicatorEnabled(false)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            if (showSidebar) {
                if (isDrawerOpen) {
                    closeDrawer()
                } else {
                    openDrawer()
                }
            } else {
                finish()
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setDrawerMenuItemChecked(menuItemId)
    }

    protected open val webView: WebView
        get() = binding.webView

    companion object {
        const val EXTRA_FILE = FileActivity.EXTRA_FILE
        const val EXTRA_TITLE = "TITLE"
        const val EXTRA_URL = "URL"
        const val EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR"
        const val EXTRA_SHOW_TOOLBAR = "SHOW_TOOLBAR"
        const val EXTRA_MENU_ITEM_ID = "MENU_ITEM_ID"
        const val EXTRA_TEMPLATE = "TEMPLATE"

        private val TAG = ExternalSiteWebView::class.java.simpleName
    }
}
