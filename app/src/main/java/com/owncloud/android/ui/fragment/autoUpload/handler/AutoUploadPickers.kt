/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.handler

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment

class AutoUploadPickers(
    private val fragment: Fragment,
    private val findPreferencesDialog: () -> SyncedFolderPreferencesDialogFragment?
) {

    private val remoteFolderLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onRemoteFolderPicked(result)
        }

    private val localFolderLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onLocalFolderPicked(result)
        }

    fun pickRemoteFolder() {
        val intent = Intent(fragment.requireContext(), FolderPickerActivity::class.java).apply {
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
        }
        remoteFolderLauncher.launch(intent)
    }

    fun pickLocalFolder() {
        val intent = Intent(fragment.requireContext(), UploadFilesActivity::class.java).apply {
            putExtra(UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE, true)
            putExtra(
                UploadFilesActivity.REQUEST_CODE_KEY,
                SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_LOCAL_FOLDER
            )
        }
        localFolderLauncher.launch(intent)
    }

    private fun onRemoteFolderPicked(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            return
        }

        val dialog = findPreferencesDialog() ?: return
        val chosenFolder = result.data?.getParcelableArgument(FolderPickerActivity.EXTRA_FOLDER, OCFile::class.java)
        dialog.setRemoteFolderSummary(chosenFolder?.remotePath)
    }

    private fun onLocalFolderPicked(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            return
        }

        val dialog = findPreferencesDialog() ?: return
        dialog.setLocalFolderSummary(result.data?.getStringExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES))
    }
}
