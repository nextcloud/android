/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.owncloud.android.R

class AutoUploadMenuProvider(
    private val onCreateCustomFolder: () -> Unit,
    private val onDeleteAllUploaded: () -> Unit
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.activity_synced_folders, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.action_create_custom_folder -> {
            onCreateCustomFolder()
            true
        }

        R.id.action_auto_upload_all_folders_delete_uploaded -> {
            onDeleteAllUploaded()
            true
        }

        else -> false
    }
}
