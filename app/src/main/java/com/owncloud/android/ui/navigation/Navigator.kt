/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation

import android.view.View
import androidx.fragment.app.FragmentManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.community.CommunityFragment

class Navigator(private val fragmentManager: FragmentManager, private val fragmentContainer: View) {
    companion object {
        private const val TAG = "Navigator"
    }

    fun push(screen: NavigatorScreen) {
        val fragment = when (screen) {
            NavigatorScreen.Community -> CommunityFragment()
        }

        if (fragmentManager.findFragmentByTag(screen.tag) != null) {
            Log_OC.d(TAG, "cannot push same fragment - ${screen.tag}")
            return
        }

        Log_OC.d(TAG, "pushing - ${screen.tag}")

        fragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment, screen.tag)
            .addToBackStack(screen::class.simpleName)
            .commit()
    }

    fun pop() {
        fragmentManager.popBackStack()
    }
}
