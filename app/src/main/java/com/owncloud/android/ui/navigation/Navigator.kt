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

class Navigator(private val fragmentManager: FragmentManager, private val fragmentContainer: View) {
    companion object {
        private const val TAG = "Navigator"
    }

    private val stack = ArrayDeque<NavigatorScreen>()

    fun push(screen: NavigatorScreen) {
        if (fragmentManager.findFragmentByTag(screen.tag) != null) {
            Log_OC.d(TAG, "cannot push same fragment - ${screen.tag}")
            return
        }

        Log_OC.d(TAG, "pushing - ${screen.tag}")

        stack.addLast(screen)
        fragmentManager.beginTransaction()
            .replace(fragmentContainer.id, screen.toFragment())
            .addToBackStack(screen.tag)
            .commit()
    }

    fun pop(): NavigatorScreen? {
        stack.removeLastOrNull()
        fragmentManager.popBackStack()
        return stack.lastOrNull()
    }

    fun getTopScreen(): NavigatorScreen? {
        val topTag = fragmentManager
            .getBackStackEntryAt(fragmentManager.backStackEntryCount - 1)
            .name
        return NavigatorScreen.fromTag(topTag)
    }
}
