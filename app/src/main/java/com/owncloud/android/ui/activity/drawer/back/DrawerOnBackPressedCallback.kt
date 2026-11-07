/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.back

import androidx.activity.OnBackPressedCallback
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment

class DrawerOnBackPressedCallback(private val activity: DrawerActivity) : OnBackPressedCallback(true) {

    override fun handleOnBackPressed() {
        if (activity.isDrawerOpen) {
            activity.closeDrawer()
            return
        }

        val fragment = activity.supportFragmentManager.findFragmentByTag(FileDetailsSharingProcessFragment.TAG)
        if (fragment is FileDetailsSharingProcessFragment) {
            fragment.onBackPressed()
            return
        }

        isEnabled = false
        activity.onBackPressedDispatcher.onBackPressed()
    }
}
