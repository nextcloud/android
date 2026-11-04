/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.navigation

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.annotation.IdRes
import androidx.core.net.toUri
import com.nextcloud.client.files.DeepLinkConstants
import com.nextcloud.utils.LinkHelper
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.navigation.NavigatorScreen

class DrawerDeepLinkHandler(
    private val activity: DrawerActivity,
    private val navigator: DrawerNavigator,
    private val menuItemClickHandler: DrawerMenuItemClickHandler
) {

    fun handle(uri: Uri) {
        val path = uri.lastPathSegment ?: return
        val deepLinkType = DeepLinkConstants.fromPath(path)
        if (deepLinkType == null) {
            SnackbarUtil.show(activity, activity.getString(R.string.invalid_url))
            return
        }

        when (deepLinkType) {
            DeepLinkConstants.OPEN_AUTO_UPLOAD -> navigator.pushFragment(NavigatorScreen.AutoUpload)
            DeepLinkConstants.OPEN_EXTERNAL_URL -> openExternalUrl(uri)
            DeepLinkConstants.ACTION_CREATE_NEW -> activity.findViewById<View>(R.id.fab_main)?.callOnClick()
            DeepLinkConstants.ACTION_APP_UPDATE -> LinkHelper.openAppStore(activity.packageName, false, activity)
            DeepLinkConstants.OPEN_NOTIFICATIONS -> navigator.pushFragment(NavigatorScreen.Notifications)
            else -> openNavigationItem(deepLinkType.navId)
        }
    }

    private fun openExternalUrl(uri: Uri) {
        val url = uri.getQueryParameter(URL_QUERY_PARAMETER) ?: return
        activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun openNavigationItem(@IdRes menuItemId: Int) {
        val menuItem = activity.drawerNavigationView?.menu?.findItem(menuItemId) ?: return
        menuItemClickHandler.onNavigationItemClicked(menuItem)
    }

    companion object {
        private const val URL_QUERY_PARAMETER = "url"
    }
}
