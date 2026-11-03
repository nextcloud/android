/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.header

import android.graphics.drawable.Drawable
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.nextcloud.utils.GlideHelper
import com.owncloud.android.R
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.drawer.navigation.DrawerNavigator
import kotlin.jvm.optionals.getOrNull

class DrawerHeaderController(private val activity: DrawerActivity, navigator: DrawerNavigator) {

    private val brandingColors = ServerBrandingColors(activity)
    private val ecosystemAppsBanner = EcosystemAppsBanner(activity, navigator, brandingColors)

    fun update(header: View) {
        val isClientBranded = activity.resources.getBoolean(R.bool.is_branded_client)
        if (!isClientBranded) {
            showServerBranding(header)
        }

        val banner = header.findViewById<ConstraintLayout>(R.id.drawer_ecosystem_apps)
        if (isClientBranded || !activity.appPreferences.isShowEcosystemApps) {
            banner.visibility = View.GONE
        } else {
            ecosystemAppsBanner.show(banner)
        }
    }

    private fun showServerBranding(header: View) {
        val account = activity.account ?: return
        val capability = activity.capabilities.getOrNull() ?: return
        if (capability.serverBackground == null) return

        val primaryColor = brandingColors.getPrimaryColor(account)
        header.findViewById<LinearLayout>(R.id.drawer_header_view).setBackgroundColor(primaryColor)

        val serverLogoURL = capability.serverLogo
        if (serverLogoURL.isNullOrEmpty() || !URLUtil.isValidUrl(serverLogoURL)) return

        GlideHelper.loadIntoTarget(
            activity,
            activity.userAccountManager.currentOwnCloudAccount,
            serverLogoURL,
            ServerLogoTarget(activity.resources, primaryColor) { setHeaderLogo(header, it, capability.serverName) },
            R.drawable.background
        )
    }

    private fun setHeaderLogo(header: View, drawable: Drawable, serverName: String?) {
        header.findViewById<ImageView>(R.id.drawer_header_logo).run {
            setImageDrawable(drawable)
            adjustViewBounds = true
        }

        if (serverName.isNullOrEmpty()) return
        header.findViewById<TextView>(R.id.drawer_header_server_name).run {
            visibility = View.VISIBLE
            text = serverName
            setTextColor(brandingColors.textColor)
        }
    }
}
