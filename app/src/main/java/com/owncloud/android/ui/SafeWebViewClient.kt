/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui

import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import com.owncloud.android.lib.common.utils.Log_OC

@Suppress("MissingOnRenderProcessGone")
open class SafeWebViewClient : WebViewClient() {

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log_OC.e(TAG, "WebView render process gone, did crash: " + detail?.didCrash())

        view?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }

        return true
    }

    companion object {
        private val TAG = SafeWebViewClient::class.java.simpleName
    }
}
