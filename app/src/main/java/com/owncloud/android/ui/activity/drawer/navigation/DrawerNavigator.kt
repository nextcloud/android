/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.navigation

import android.content.Intent
import android.view.MenuItem
import androidx.fragment.app.commit
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeActivity.ComposeDestination
import com.nextcloud.utils.extensions.navigateToAllFiles
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.activity.UserInfoActivity
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.ui.navigation.model.ActionBarStyle
import org.greenrobot.eventbus.EventBus
import kotlin.jvm.optionals.getOrNull

class DrawerNavigator(private val activity: DrawerActivity) {

    fun pushFragment(screen: NavigatorScreen) {
        if (activity is NavigatorActivity) {
            activity.push(screen)
            return
        }
        activity.startActivity(NavigatorActivity.intent(activity, screen))
    }

    fun startPhotoSearch() {
        MainApp.showOnlyFilesOnDevice(false)
        launchActivityForSearch(SearchEvent(PHOTO_SEARCH_MIME_TYPE, SearchRemoteOperation.SearchType.PHOTO_SEARCH))
    }

    fun showFiles(onDeviceOnly: Boolean, onlyPersonalFiles: Boolean) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly)
        MainApp.showOnlyPersonalFiles(onlyPersonalFiles)
        activity.startActivity(fileDisplayActivityIntent(FileDisplayActivity.ALL_FILES))
    }

    fun showOnDeviceFiles() {
        MainApp.showOnlyFilesOnDevice(true)
        MainApp.showOnlyPersonalFiles(false)
        activity.startActivity(fileDisplayActivityIntent(FileDisplayActivity.ON_DEVICE))
    }

    fun replaceAlbumFragment() {
        if (activity.isAlbumsFragment) return
        activity.supportFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.left_fragment_container, AlbumsFragment.newInstance(false), AlbumsFragment.TAG)
        }
    }

    fun restartFileDisplayActivity() {
        activity.startActivity(fileDisplayActivityIntent(FileDisplayActivity.RESTART))
    }

    fun openAllFiles(onlyPersonal: Boolean) {
        activity.closeDrawer()
        activity.navigateToAllFiles(onlyPersonal)
        EventBus.getDefault().post(ChangeMenuEvent())
    }

    fun openFavoritesTab() {
        resetOnlyPersonalAndOnDevice()
        activity.showActionBar(ActionBarStyle.Plain)
        launchActivityForSearch(SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH))
    }

    fun openMediaTab() {
        GalleryFragment.clearSavedViewState()
        resetOnlyPersonalAndOnDevice()
        activity.showActionBar(ActionBarStyle.Plain)
        startPhotoSearch()
    }

    fun openAlbums() {
        if (activity is FileDisplayActivity) {
            replaceAlbumFragment()
        } else {
            activity.startActivity(fileDisplayActivityIntent(FileDisplayActivity.ALBUMS))
        }
    }

    fun openNavigatorScreen(screen: NavigatorScreen) {
        resetOnlyPersonalAndOnDevice()
        pushFragment(screen)
    }

    fun openSettings() {
        resetOnlyPersonalAndOnDevice()
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    fun openAccountRemovalDialog(menuItem: MenuItem) {
        resetOnlyPersonalAndOnDevice()
        val uncheckedMenuItem = menuItem.setChecked(false)
        Log_OC.d(TAG, "onNavigationItemClicked nav_logout setChecked $uncheckedMenuItem")
        activity.user.getOrNull()?.let {
            UserInfoActivity.openAccountRemovalDialog(it, activity.supportFragmentManager)
        }
    }

    fun openSharedTab() {
        resetOnlyPersonalAndOnDevice()
        launchActivityForSearch(SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER))
    }

    fun openRecentFiles() {
        resetOnlyPersonalAndOnDevice()
        launchActivityForSearch(SearchEvent("", SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH))
    }

    fun openAssistant() {
        resetOnlyPersonalAndOnDevice()
        startAssistantScreen()
    }

    fun openGroupfolders() {
        resetOnlyPersonalAndOnDevice()
        activity.startActivity(fileDisplayActivityIntent(FileDisplayActivity.LIST_GROUPFOLDERS))
    }

    fun startAssistantScreen() {
        val intent = Intent(activity.applicationContext, ComposeActivity::class.java)
            .putExtra(ComposeActivity.DESTINATION, ComposeDestination.getAssistantScreen(activity))
        activity.startActivity(intent)
    }

    fun resetOnlyPersonalAndOnDevice() {
        MainApp.showOnlyFilesOnDevice(false)
        MainApp.showOnlyPersonalFiles(false)
    }

    private fun launchActivityForSearch(searchEvent: SearchEvent) {
        val intent = fileDisplayActivityIntent(Intent.ACTION_SEARCH)
            .putExtra(OCFileListFragment.SEARCH_EVENT, searchEvent)
        activity.startActivity(intent)
    }

    private fun fileDisplayActivityIntent(action: String): Intent =
        Intent(activity.applicationContext, FileDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.action = action
        }

    companion object {
        private const val TAG = "DrawerNavigator"
        private const val PHOTO_SEARCH_MIME_TYPE = "image/%"
    }
}
