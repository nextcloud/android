/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.fragment

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.nextcloud.utils.extensions.lastFragment
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.albums.AlbumItemsFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import java.util.Optional

class DrawerFragmentFinder(private val activity: AppCompatActivity) {

    private val fragmentManager: FragmentManager
        get() = activity.supportFragmentManager

    val ocFileListFragment: OCFileListFragment?
        get() = activity.lastFragment() as? OCFileListFragment
            ?: fragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES) as? OCFileListFragment

    val isAlbumsFragmentVisible: Boolean
        get() = isFragmentVisible<AlbumsFragment>(AlbumsFragment.TAG)

    val isAlbumItemsFragmentVisible: Boolean
        get() = isFragmentVisible<AlbumItemsFragment>(AlbumItemsFragment.TAG)

    fun <T : Fragment> getFragment(tag: String?, clazz: Class<T>): Optional<T> =
        Optional.ofNullable(fragmentManager.findFragmentByTag(tag))
            .filter(clazz::isInstance)
            .map(clazz::cast)

    private inline fun <reified T : Fragment> isFragmentVisible(tag: String): Boolean =
        (fragmentManager.findFragmentByTag(tag) as? T)?.isVisible == true
}
