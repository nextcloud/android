/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.navigation

import android.content.res.Resources
import android.view.Menu
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.DrawerMenuUtil

object DrawerMenuFilter {

    fun filter(menu: Menu, user: User, capability: OCCapability?, resources: Resources) {
        capability?.let {
            DrawerMenuUtil.filterTrashbinMenuItem(menu, it)
            DrawerMenuUtil.filterActivityMenuItem(menu, it)
            DrawerMenuUtil.filterGroupfoldersMenuItem(menu, it)
            DrawerMenuUtil.filterAssistantMenuItem(menu, it, resources)
        }

        DrawerMenuUtil.filterSearchMenuItems(menu, user)
        DrawerMenuUtil.setupHomeMenuItem(menu, resources)
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_community, !resources.getBoolean(R.bool.participate_enabled))
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_shared, !resources.getBoolean(R.bool.shared_enabled))
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_logout, !resources.getBoolean(R.bool.show_drawer_logout))
    }
}
