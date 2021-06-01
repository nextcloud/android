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
import android.os.Bundle
import android.widget.Toast
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.device.DeviceInfo
import com.owncloud.android.R
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.ui.asynctasks.TextEditorLoadUrlTask
import javax.inject.Inject

class TextEditorWebView : EditorWebView() {
    @Inject
    lateinit var appInfo: AppInfo
    @Inject
    lateinit var deviceInfo: DeviceInfo

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used > Lollipop
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!user.isPresent) {
            Toast.makeText(this, getString(R.string.failed_to_start_editor), Toast.LENGTH_LONG).show()
            finish()
        }

        val editor = FileMenuFilter.getEditor(contentResolver, user.get(), file.mimeType)

        if (editor != null && editor.id == "onlyoffice") {
            webview.settings.userAgentString = generateOnlyOfficeUserAgent()
        }

        webview.addJavascriptInterface(MobileInterface(), "DirectEditingMobileInterface")

        webview.setDownloadListener { url, _, _, _, _ -> downloadFile(Uri.parse(url)) }

        loadUrl(intent.getStringExtra(ExternalSiteWebView.EXTRA_URL))
    }

    override fun loadUrl(url: String?) {
        if (url.isNullOrEmpty()) {
            TextEditorLoadUrlTask(this, user.get(), file).execute()
        }
    }

    private fun generateOnlyOfficeUserAgent(): String {
        val userAgent = applicationContext.resources.getString(R.string.only_office_user_agent)

        return String.format(userAgent, deviceInfo.androidVersion, appInfo.getAppVersion(this))
    }
}
