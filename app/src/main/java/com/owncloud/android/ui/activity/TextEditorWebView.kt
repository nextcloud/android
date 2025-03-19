/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.nextcloud.android.common.ui.util.PlatformThemeUtil
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.R
import com.owncloud.android.ui.asynctasks.TextEditorLoadUrlTask
import com.owncloud.android.utils.theme.ThemeUtils
import javax.inject.Inject

class TextEditorWebView : EditorWebView() {
    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var deviceInfo: DeviceInfo

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var editorUtils: EditorUtils

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used > Lollipop
    override fun postOnCreate() {
        super.postOnCreate()

        if (!user.isPresent) {
            Toast.makeText(this, getString(R.string.failed_to_start_editor), Toast.LENGTH_LONG).show()
            finish()
        }

        val editor = editorUtils.getEditor(user.get(), file.mimeType)

        if (editor != null && editor.id == "onlyoffice") {
            webView.settings.userAgentString = generateOnlyOfficeUserAgent()
        }

        webView.addJavascriptInterface(MobileInterface(), "DirectEditingMobileInterface")

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                webView.settings,
                WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
            )
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && PlatformThemeUtil.isDarkMode(this)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }

        webView.setDownloadListener { url, _, _, _, _ -> downloadFile(url.toUri()) }

        loadUrl(intent.getStringExtra(EXTRA_URL))
    }

    override fun loadUrl(url: String?) {
        if (url.isNullOrEmpty()) {
            TextEditorLoadUrlTask(this, user.get(), file, editorUtils).execute()
        }
    }

    private fun generateOnlyOfficeUserAgent(): String {
        val userAgent = applicationContext.resources.getString(R.string.only_office_user_agent)

        return String.format(userAgent, deviceInfo.androidVersion, appInfo.getAppVersion(this))
    }
}
