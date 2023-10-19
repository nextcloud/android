/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.net.Uri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.snackbar.Snackbar
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
            Snackbar.make(binding.root, getString(R.string.failed_to_start_editor), Snackbar.LENGTH_LONG).show()
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

        webView.setDownloadListener { url, _, _, _, _ -> downloadFile(Uri.parse(url)) }

        loadUrl(null)
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
