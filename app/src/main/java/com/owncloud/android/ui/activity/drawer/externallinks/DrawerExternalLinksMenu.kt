/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.externallinks

import android.graphics.drawable.Drawable
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.google.android.material.navigation.NavigationView
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.utils.DrawableUtil

class DrawerExternalLinksMenu(
    private val activity: DrawerActivity,
    private val externalLinkOpener: ExternalLinkOpener
) {

    fun update() {
        val navigationView = activity.drawerNavigationView ?: return
        if (!MDMConfig.externalSiteSupport(activity)) return

        navigationView.menu.removeGroup(R.id.drawer_menu_external_links)

        val greyColor = ContextCompat.getColor(activity, R.color.drawer_menu_icon)
        activity.externalLinksProvider.getExternalLink(ExternalLinkType.LINK) { externalLinks ->
            externalLinks.forEach { addMenuItem(navigationView, it, greyColor) }
        }
    }

    fun onExternalLinkClicked(menuItem: MenuItem) {
        val linkId = menuItem.itemId - MENU_ITEM_EXTERNAL_LINK
        activity.externalLinksProvider.getExternalLink(ExternalLinkType.LINK) { externalLinks ->
            val link = externalLinks.firstOrNull { it.id == linkId }
            if (link == null) {
                Log_OC.w(TAG, "No external link found for menu item: " + menuItem.title)
            } else {
                externalLinkOpener.open(link)
            }
        }
    }

    private fun addMenuItem(navigationView: NavigationView, link: ExternalLink, tintColor: Int) {
        val menuItemId = navigationView.menu
            .add(
                R.id.drawer_menu_external_links,
                MENU_ITEM_EXTERNAL_LINK + link.id,
                MENU_ORDER_EXTERNAL_LINKS,
                link.name
            )
            .setCheckable(true)
            .itemId

        GlideHelper.loadIntoTarget(
            activity,
            activity.userAccountManager.currentOwnCloudAccount,
            link.iconUrl,
            ExternalLinkMenuIconTarget { setMenuItemIcon(navigationView, menuItemId, it, tintColor) },
            R.drawable.ic_link
        )
    }

    private fun setMenuItemIcon(navigationView: NavigationView, menuItemId: Int, drawable: Drawable?, tintColor: Int) {
        val menuItem = navigationView.menu.findItem(menuItemId) ?: return
        if (drawable == null) {
            menuItem.setIcon(R.drawable.ic_link)
            return
        }

        val resizedDrawable = DrawableUtil.getResizedDrawable(activity, drawable, ICON_SIZE_PX)
        menuItem.icon = activity.viewThemeUtils.platform.colorDrawable(resizedDrawable, tintColor)
    }

    companion object {
        private const val TAG = "DrawerExternalLinksMenu"
        private const val MENU_ORDER_EXTERNAL_LINKS = 3
        private const val MENU_ITEM_EXTERNAL_LINK = 111
        private const val ICON_SIZE_PX = 32
    }
}
