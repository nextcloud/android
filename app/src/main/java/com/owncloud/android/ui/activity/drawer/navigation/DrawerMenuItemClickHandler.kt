/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.navigation

import android.view.MenuItem
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.utils.extensions.navigateToAllFiles
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.drawer.externallinks.DrawerExternalLinksMenu
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.navigation.NavigatorScreen
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.greenrobot.eventbus.EventBus

class DrawerMenuItemClickHandler(
    private val activity: DrawerActivity,
    private val navigator: DrawerNavigator,
    private val externalLinksMenu: DrawerExternalLinksMenu
) {

    fun onNavigationItemClicked(menuItem: MenuItem) {
        val itemId = menuItem.itemId
        when (itemId) {
            R.id.nav_all_files, R.id.nav_personal_files -> navigator.openAllFiles(itemId == R.id.nav_personal_files)
            R.id.nav_favorites -> navigator.openFavoritesTab()
            R.id.nav_gallery -> navigator.openMediaTab()
            R.id.nav_album -> navigator.openAlbums()
            R.id.nav_on_device -> navigator.showOnDeviceFiles()
            in navigatorScreensByMenuItemId -> navigatorScreensByMenuItemId[itemId]?.let(navigator::openNavigatorScreen)
            R.id.nav_settings -> navigator.openSettings()
            R.id.nav_logout -> navigator.openAccountRemovalDialog(menuItem)
            R.id.nav_shared -> navigator.openSharedTab()
            R.id.nav_recent_files -> navigator.openRecentFiles()
            R.id.nav_assistant -> navigator.openAssistant()
            R.id.nav_groupfolders -> navigator.openGroupfolders()
            else -> onOtherNavigationItemClicked(menuItem)
        }

        activity.ocFileListFragment?.resetFileDepth()
        activity.highlightNavigationViewItem(itemId)
    }

    @SuppressFBWarnings("RV")
    fun onBottomNavigationItemSelected(menuItem: MenuItem): Boolean {
        val itemId = menuItem.itemId
        activity.ocFileListFragment?.exitSelectionMode()
        navigator.resetOnlyPersonalAndOnDevice()

        when (itemId) {
            R.id.nav_all_files -> {
                activity.navigateToAllFiles()
                EventBus.getDefault().post(ChangeMenuEvent())
            }

            R.id.nav_favorites -> navigator.openFavoritesTab()

            R.id.nav_assistant -> if (activity !is ComposeActivity) navigator.startAssistantScreen()

            R.id.nav_gallery -> navigator.openMediaTab()

            R.id.nav_album -> navigator.openAlbums()
        }

        activity.supportActionBar?.setIcon(null)
        activity.ocFileListFragment?.resetFileDepth()
        activity.highlightNavigationViewItem(itemId)
        return false
    }

    private fun onOtherNavigationItemClicked(menuItem: MenuItem) {
        if (menuItem.groupId == R.id.drawer_menu_external_links) {
            externalLinksMenu.onExternalLinkClicked(menuItem)
            return
        }
        Log_OC.w(TAG, "Unknown drawer menu item clicked: " + menuItem.title)
    }

    companion object {
        private const val TAG = "DrawerMenuItemClickHandler"

        private val navigatorScreensByMenuItemId: Map<Int, NavigatorScreen> by lazy {
            mapOf(
                R.id.nav_uploads to NavigatorScreen.UploadList,
                R.id.nav_trashbin to NavigatorScreen.Trashbin,
                R.id.nav_activity to NavigatorScreen.Activities,
                R.id.nav_community to NavigatorScreen.Community
            )
        }
    }
}
