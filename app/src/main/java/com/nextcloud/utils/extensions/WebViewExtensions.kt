/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Proxy
import android.text.TextUtils
import android.util.ArrayMap
import android.util.Log
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import java.io.PrintWriter
import java.io.StringWriter

private const val PROXY_TAG = "PROXY"
private const val WEB_VIEW_TAG = "WEB_VIEW"

fun WebView.getProxyHost() = OwnCloudClientManagerFactory.getProxyHost() ?: ""
fun WebView.getProxyPort() = OwnCloudClientManagerFactory.getProxyPort()

/**
 * From https://stackoverflow.com/a/18453384
 *
 * @return
 */
@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
@Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
fun WebView.setProxyKKPlus() {
    if (TextUtils.isEmpty(getProxyHost()) || getProxyPort() <= 0) {
        return
    }

    val applicationClassName = "android.app.Application"
    Log.d(PROXY_TAG, "Setting proxy with >= 4.4 API.")

    val appContext = context.applicationContext

    System.setProperty("http.proxyHost", getProxyHost())
    System.setProperty("http.proxyPort", getProxyPort().toString())
    System.setProperty("https.proxyHost", getProxyHost())
    System.setProperty("https.proxyPort", getProxyPort().toString())
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

fun WebView.hasProxyConfig(): Boolean = getProxyHost().isNotEmpty()

@Suppress("TooGenericExceptionCaught")
fun Context.isWebViewAvailable(): Boolean = try {
    WebViewCompat.getCurrentWebViewPackage(this) != null
} catch (e: Exception) {
    Log.w(WEB_VIEW_TAG, "WebView provider cannot be resolved: ${e.message}")
    false
}
