/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.fileactions

import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper

class FileActionHandler(
    private val containerActivity: FileFragment.ContainerActivity,
    private val fragmentManager: FragmentManager,
    private val backgroundJobManager: BackgroundJobManager,
    private val onFavoriteChanged: (isFavorite: Boolean) -> Unit = {}
) {
    private val fileOperationsHelper: FileOperationsHelper?
        get() = containerActivity.fileOperationsHelper

    fun onFileActionChosen(@IdRes itemId: Int, file: OCFile, parentFolder: OCFile?, view: View?) {
        when (itemId) {
            R.id.action_send_file -> fileOperationsHelper?.sendShareFile(file, true)

            R.id.action_open_file_with -> fileOperationsHelper?.openFile(file)

            R.id.action_remove_file -> RemoveFilesDialogFragment.newInstance(file)
                .show(fragmentManager, FTAG_REMOVE_CONFIRMATION)

            R.id.action_rename_file -> RenameFileDialogFragment.newInstance(file, parentFolder)
                .show(fragmentManager, FileDetailFragment.FTAG_RENAME_FILE)

            R.id.action_cancel_sync -> (containerActivity as? FileDisplayActivity)?.cancelTransference(file)

            R.id.action_download_file, R.id.action_sync_file -> syncFile(file)

            R.id.action_export_file -> fileOperationsHelper?.exportFiles(
                arrayListOf(file),
                view?.context,
                view,
                backgroundJobManager
            )

            R.id.action_set_as_wallpaper -> fileOperationsHelper?.setPictureAs(file, view)

            R.id.action_retry -> backgroundJobManager.startOfflineOperations()

            R.id.action_favorite -> setFavorite(file, true)

            R.id.action_unset_favorite -> setFavorite(file, false)
        }
    }

    private fun syncFile(file: OCFile) {
        (containerActivity as? FileActivity)?.showSyncLoadingDialog(file.isFolder)
        fileOperationsHelper?.syncFileOrFolder(file)
    }

    private fun setFavorite(file: OCFile, isFavorite: Boolean) {
        fileOperationsHelper?.toggleFavoriteFile(file, isFavorite)
        onFavoriteChanged(isFavorite)
    }

    companion object {
        private const val FTAG_REMOVE_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT"
    }
}
