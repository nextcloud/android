/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.nextcloud.android.common.ui.util.PlatformThemeUtil
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.device.DeviceInfo
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.asynctasks.TextEditorLoadUrlTask
import com.owncloud.android.utils.DisplayUtils
import javax.inject.Inject

class TextEditorWebView : EditorWebView() {
    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var deviceInfo: DeviceInfo

    companion object {
        fun startTextEditor(file: OCFile?, context: Context?) {
            val context = context ?: return
            val file = file ?: return

            Intent(context, TextEditorWebView::class.java).apply {
                putExtra(EXTRA_TITLE, "Text")
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_SHOW_SIDEBAR, false)
            }.also {
                context.startActivity(it)
            }
        }
    }

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used > Lollipop
    override fun postOnCreate() {
        super.postOnCreate()

        if (!user.isPresent) {
            DisplayUtils.showSnackMessage(this, R.string.failed_to_start_editor)
            finish()
        }

        user.ifPresent {
            val editor = editorUtils.getAvailableEditor(it, file?.mimeType)

            if (editorUtils.usesOfficeUserAgent(editor)) {
                webView.settings.userAgentString = generateOfficeUserAgent()
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

            webView.setDownloadListener { url, _, _, _, _ -> downloadFile(url.toUri(), fileName) }
            loadUrl(intent.getStringExtra(EXTRA_URL))
        }
    }

    override fun loadUrl(url: String?) {
        if (url.isNullOrEmpty()) {
            user.ifPresent {
                TextEditorLoadUrlTask(this, it, file, editorUtils).execute()
            }
        }
    }

    private fun generateOfficeUserAgent(): String {
        val userAgent = applicationContext.resources.getString(R.string.office_user_agent)
        return String.format(userAgent, deviceInfo.androidVersion, appInfo.getAppVersion(this))
    }
}
