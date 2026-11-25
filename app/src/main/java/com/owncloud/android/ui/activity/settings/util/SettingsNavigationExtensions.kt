/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.util

import android.app.Activity
import android.content.Intent
import com.owncloud.android.ui.activity.ExtendedSettingsActivity
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen

fun Activity.startExtendedSettingsDialog(dialog: ExtendedSettingsActivityDialog, dismissable: Boolean = true) {
    val intent = ExtendedSettingsActivity.createIntent(this, dialog, dismissable)

    startActivityForResult(intent, dialog.resultId)
}

fun Activity.startExternalSiteWebView(title: String?, url: String?) {
    val intent = Intent(applicationContext, ExternalSiteWebView::class.java)
        .putExtra(ExternalSiteWebView.EXTRA_TITLE, title)
        .putExtra(ExternalSiteWebView.EXTRA_URL, url)
        .putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)

    startActivity(intent)
}

fun Activity.startNavigatorScreen(screen: NavigatorScreen) {
    startActivity(NavigatorActivity.intent(this, screen))
}
