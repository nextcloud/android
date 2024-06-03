/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Proxy
import android.net.Uri
import android.text.TextUtils
import android.util.ArrayMap
import android.util.Log
import android.webkit.WebView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import java.io.PrintWriter
import java.io.StringWriter

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

    /**
     * From https://stackoverflow.com/a/18453384
     *
     * @return
     */
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    fun setProxyKKPlus(webView: WebView) {
        val proxyHost = OwnCloudClientManagerFactory.getProxyHost()
        val proxyPort = OwnCloudClientManagerFactory.getProxyPort()

        if (TextUtils.isEmpty(proxyHost) || proxyPort <= 0) {
            return
        }

        val applicationClassName = "android.app.Application"
        Log.d(PROXY_TAG, "Setting proxy with >= 4.4 API.")

        val appContext = webView.context.applicationContext

        System.setProperty("http.proxyHost", proxyHost)
        System.setProperty("http.proxyPort", proxyPort.toString())
        System.setProperty("https.proxyHost", proxyHost)
        System.setProperty("https.proxyPort", proxyPort.toString())
        try {
            val applicationClass = Class.forName(applicationClassName)
            val loadedApkField = applicationClass.getField("mLoadedApk")
            loadedApkField.isAccessible = true
            val loadedApk = loadedApkField[appContext]
            val loadedApkCls = Class.forName("android.app.LoadedApk")
            val receiversField = loadedApkCls.getDeclaredField("mReceivers")
            receiversField.isAccessible = true
            val receivers = receiversField[loadedApk] as ArrayMap<*, *>
            for (receiverMap in receivers.values) {
                for (rec in (receiverMap as ArrayMap<*, *>).keys) {
                    val clazz: Class<*> = rec.javaClass
                    if (clazz.name.contains("ProxyChangeListener")) {
                        val onReceiveMethod = clazz.getDeclaredMethod(
                            "onReceive",
                            Context::class.java,
                            Intent::class.java
                        )
                        val intent = Intent(Proxy.PROXY_CHANGE_ACTION)
                        onReceiveMethod.invoke(rec, appContext, intent)
                    }
                }
            }
            Log.d(PROXY_TAG, "Setting proxy with >= 4.4 API successful!")
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val exceptionAsString = sw.toString()
            e.message?.let { Log.v(PROXY_TAG, it) }
            Log.v(PROXY_TAG, exceptionAsString)
        }
    }

    companion object {
        private const val PROXY_TAG = "PROXY"
    }
}
