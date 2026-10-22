/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import java.lang.ref.WeakReference

class ProxyUtil(private val context: WeakReference<Context>) {
    companion object {
        private const val TAG = "ProxyUtil"
    }

    @SuppressLint("RequiresFeature", "ReturnCount")
    fun applyProxy(onApplied: Runnable) {
        val context = context.get() ?: return
        val proxyHost = OwnCloudClientManagerFactory.getProxyHost()
        val proxyPort = OwnCloudClientManagerFactory.getProxyPort()

        if (proxyHost.isNullOrEmpty() || proxyPort <= 0) {
            onApplied.run()
            return
        }

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log_OC.w(TAG, "WebView does not support proxy override")
            onApplied.run()
            return
        }

        val proxyConfig = ProxyConfig.Builder()
            .addProxyRule("$proxyHost:$proxyPort")
            .build()

        ProxyController.getInstance().setProxyOverride(
            proxyConfig,
            ContextCompat.getMainExecutor(context),
            onApplied
        )
    }
}
