/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation

import android.view.View
import androidx.fragment.app.FragmentManager
import com.owncloud.android.ui.fragment.community.CommunityFragment

class Navigator(private val fragmentManager: FragmentManager, private val fragmentContainer: View) {
    fun push(screen: NavigatorScreen) {
        val fragment = when (screen) {
            NavigatorScreen.Community -> CommunityFragment()
        }
        fragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
            .addToBackStack(screen::class.simpleName)
            .commit()
    }

    fun pop() {
        fragmentManager.popBackStack()
    }
}
