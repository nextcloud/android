/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.owncloud.android.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R

class WebViewUtil(private val context: Context) {

    private val packageName = "com.google.android.webview"

    fun checkWebViewVersion() {
        if (!isWebViewVersionValid()) {
            showUpdateDialog()
        }
    }

    private fun isWebViewVersionValid(): Boolean {
        val currentWebViewVersion = getCurrentWebViewMajorVersion() ?: return true
        val minSupportedWebViewVersion: String = getMinimumSupportedMajorWebViewVersion()
        return currentWebViewVersion.toInt() >= minSupportedWebViewVersion.toInt()
    }

    private fun showUpdateDialog() {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.webview_version_check_alert_dialog_title))
            .setMessage(context.getString(R.string.webview_version_check_alert_dialog_message))
            .setCancelable(false)
            .setPositiveButton(
                context.getString(R.string.webview_version_check_alert_dialog_positive_button_title)
            ) { _, _ ->
                redirectToAndroidSystemWebViewStorePage()
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun redirectToAndroidSystemWebViewStorePage() {
        val uri = Uri.parse("market://details?id=$packageName")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            redirectToPlayStoreWebsiteForAndroidSystemWebView()
        }
    }

    private fun redirectToPlayStoreWebsiteForAndroidSystemWebView() {
        val playStoreWebUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        val webIntent = Intent(Intent.ACTION_VIEW, playStoreWebUri)
        context.startActivity(webIntent)
    }

    private fun getCurrentWebViewMajorVersion(): String? {
        val pm: PackageManager = context.packageManager

        return try {
            val pi = pm.getPackageInfo("com.google.android.webview", 0)
            val fullVersion = pi.versionName

            // Split the version string by "." and get the first part
            val versionParts = fullVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()

            if (versionParts.isNotEmpty()) {
                versionParts[0]
            } else {
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Ideally we should fetch from database, reading actual value
     * from PlayStore not feasible due to frequently api changes made by
     * Google
     *
     */
    private fun getMinimumSupportedMajorWebViewVersion(): String {
        return "118"
    }
}
