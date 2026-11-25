/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.internalTwoWaySync.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.owncloud.android.BuildConfig
import com.owncloud.android.R

class InternalTwoWaySyncMenuProvider(
    private val onDisableForAllFoldersItemCreated: (MenuItem?) -> Unit,
    private val onDisableForAllFolders: () -> Unit,
    private val onRunNow: () -> Unit
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.activity_internal_two_way_sync, menu)
        menu.findItem(R.id.action_run_two_way_sync)?.isVisible = BuildConfig.DEBUG
        onDisableForAllFoldersItemCreated(menu.findItem(R.id.action_dismiss_two_way_sync))
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.action_dismiss_two_way_sync -> {
            onDisableForAllFolders()
            true
        }

        R.id.action_run_two_way_sync -> {
            onRunNow()
            true
        }

        else -> false
    }
}
