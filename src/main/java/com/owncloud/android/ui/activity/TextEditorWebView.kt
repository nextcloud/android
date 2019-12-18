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
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.owncloud.android.R
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.lib.common.utils.Log_OC

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class TextEditorWebView : EditorWebView() {

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used >= Lollipop
    // suppress warning as webview is only used >= Lollipop
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editor = FileMenuFilter.getEditor(contentResolver, account, file.mimeType)

        if (editor != null && editor.id == "onlyoffice") {
            webview.settings.userAgentString = generateOnlyOfficeUserAgent()
        }

        webview.addJavascriptInterface(MobileInterface(), "DirectEditingMobileInterface")

        loadUrl(intent.getStringExtra(ExternalSiteWebView.EXTRA_URL), file)
    }

    private fun generateOnlyOfficeUserAgent(): String {
        val appString = applicationContext.resources.getString(R.string.only_office_user_agent)
        val packageName = applicationContext.packageName
        val androidVersion = Build.VERSION.RELEASE
        var appVersion = ""
        try {
            val pInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
            if (pInfo != null) {
                appVersion = pInfo.versionName
            }
        } catch (e: NameNotFoundException) {
            Log_OC.e(this, "Trying to get packageName", e.cause)
        }
        return String.format(appString, androidVersion, appVersion)
    }
}
