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
import android.net.Proxy
import android.text.TextUtils
import android.util.ArrayMap
import android.util.Log
import android.webkit.WebView
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import java.io.PrintWriter
import java.io.StringWriter

class WebViewUtil {

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
