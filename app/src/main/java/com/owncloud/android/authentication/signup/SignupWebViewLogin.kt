/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.signup

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.RawResourceReader
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountSetupWebviewBinding
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.utils.ProxyUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.lang.ref.WeakReference

class SignupWebViewLogin(
    private val activity: FragmentActivity,
    private val viewThemeUtils: ViewThemeUtils,
    private val currentBinding: () -> AccountSetupWebviewBinding?,
    private val loginDataPrefix: String,
    private val onLoginDataUrl: (String) -> Unit
) {

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    fun start(binding: AccountSetupWebviewBinding) {
        viewThemeUtils.platform.colorCircularProgressBar(
            binding.loginWebviewProgressBar,
            ColorRole.ON_PRIMARY_CONTAINER
        )
        binding.loginWebview.visibility = View.GONE
        binding.loginWebview.settings.run {
            allowFileAccess = false
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = MainApp.getUserAgent()
            saveFormData = false
            savePassword = false
        }

        val headers = mapOf(RemoteOperation.OCS_API_HEADER to RemoteOperation.OCS_API_HEADER_VALUE)
        ProxyUtil(WeakReference<Context>(activity)).applyProxy {
            currentBinding()?.loginWebview?.loadUrl(activity.getString(R.string.provider_registration_server), headers)
        }
        binding.loginFlowV2.loginFlowInfoV2.visibility = View.GONE

        setWebViewClient(binding)
    }

    private fun setWebViewClient(binding: AccountSetupWebviewBinding) {
        binding.loginWebview.webViewClient = object : NextcloudWebViewClient(activity.supportFragmentManager) {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (!url.startsWith(loginDataPrefix)) {
                    return false
                }
                onLoginDataUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                showLoadedWebView()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                showLoadedWebView()

                val customError = RawResourceReader.readText(activity.resources, R.raw.custom_error)
                if (customError.isNotEmpty()) {
                    currentBinding()?.loginWebview?.loadData(customError, HTML_MIME_TYPE, null)
                }
            }
        }
    }

    private fun showLoadedWebView() {
        currentBinding()?.run {
            loginWebviewProgressBar.visibility = View.GONE
            loginWebview.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val HTML_MIME_TYPE = "text/html; charset=UTF-8"
    }
}
