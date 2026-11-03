/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.externallinks

import android.app.Activity
import android.content.Intent
import com.nextcloud.client.utils.IntentUtil
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.ui.activity.ExternalSiteWebView

class ExternalLinkOpener(private val activity: Activity) {

    fun open(link: ExternalLink) {
        if (link.redirect) {
            IntentUtil.startLinkIntent(activity, link.url)
            return
        }
        openInWebView(link)
    }

    fun openInWebView(link: ExternalLink) {
        val intent = Intent(activity.applicationContext, ExternalSiteWebView::class.java)
            .putExtra(ExternalSiteWebView.EXTRA_TITLE, link.name)
            .putExtra(ExternalSiteWebView.EXTRA_URL, link.url)
            .putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true)
        activity.startActivity(intent)
    }
}
