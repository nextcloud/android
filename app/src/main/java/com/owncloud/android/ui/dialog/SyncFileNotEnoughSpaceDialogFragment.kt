/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.storage.StorageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.utils.DisplayUtils

class SyncFileNotEnoughSpaceDialogFragment :
    ConfirmationDialogFragment(),
    ConfirmationDialogFragmentListener {

    private var targetFile: OCFile? = null

    private val storageActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op */ }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        targetFile = requireArguments().getParcelableArgument(ARG_PASSED_FILE, OCFile::class.java)
        setOnConfirmationListener(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onConfirmation(callerTag: String?) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putParcelable(RESULT_FILE, targetFile)
                putString(RESULT_ACTION, ACTION_CHOOSE)
            }
        )
    }

    override fun onNeutral(callerTag: String?) = Unit

    override fun onCancel(callerTag: String?) {
        storageActivityResult.launch(Intent(StorageManager.ACTION_MANAGE_STORAGE))
    }

    companion object {
        const val REQUEST_KEY = "SyncFileNotEnoughSpaceDialogFragment"
        const val RESULT_FILE = "result_file"
        const val RESULT_ACTION = "result_action"
        const val ACTION_CHOOSE = "action_choose"

        private const val ARG_PASSED_FILE = "fragment_parent_caller"

        @JvmStatic
        fun newInstance(file: OCFile, availableDeviceSpace: Long) = SyncFileNotEnoughSpaceDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TITLE_ID, R.string.sync_not_enough_space_dialog_title)
                putInt(ARG_MESSAGE_RESOURCE_ID, R.string.sync_not_enough_space_dialog_placeholder)
                putStringArray(
                    ARG_MESSAGE_ARGUMENTS,
                    arrayOf(
                        file.fileName,
                        DisplayUtils.bytesToHumanReadable(file.fileLength),
                        DisplayUtils.bytesToHumanReadable(availableDeviceSpace)
                    )
                )
                putParcelable(ARG_PASSED_FILE, file)
                if (file.isFolder) putInt(ARG_POSITIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_choose)
                putInt(ARG_NEGATIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_free_space)
                putInt(ARG_NEUTRAL_BTN_RES, R.string.common_cancel)
            }
        }
    }
}
