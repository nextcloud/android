/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.uploadList

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.upload.AlbumFileUploadWorker
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.UploadsStorageManager

class UploadListMenuProvider(
    private val context: Context,
    private val preferences: AppPreferences,
    private val userAccountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private val onGlobalPauseToggled: () -> Unit
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.activity_upload_list, menu)
        menu.findItem(R.id.action_toggle_global_pause)?.let { updateGlobalPauseIcon(it) }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId != R.id.action_toggle_global_pause) {
            return false
        }

        toggleGlobalPause(menuItem)
        return true
    }

    private fun updateGlobalPauseIcon(item: MenuItem) {
        val paused = preferences.isGlobalUploadPaused()
        item.setIcon(if (paused) R.drawable.ic_global_resume else R.drawable.ic_global_pause)
        item.title = context.getString(
            if (paused) {
                R.string.upload_action_global_upload_resume
            } else {
                R.string.upload_action_global_upload_pause
            }
        )
    }

    private fun toggleGlobalPause(item: MenuItem) {
        val paused = !preferences.isGlobalUploadPaused()
        preferences.setGlobalUploadPaused(paused)
        updateGlobalPauseIcon(item)

        if (paused) {
            FileUploadWorker.pauseActiveUploads()
            AlbumFileUploadWorker.pauseActiveUploads()
        }

        val uploadHelper = FileUploadHelper.instance()
        userAccountManager.allUsers.filterNotNull().forEach { user ->
            val ids = uploadsStorageManager.getCurrentUploadIds(user.accountName)
            uploadHelper.cancelAndRestartUploadJob(user, ids)
        }
        onGlobalPauseToggled()
    }
}
