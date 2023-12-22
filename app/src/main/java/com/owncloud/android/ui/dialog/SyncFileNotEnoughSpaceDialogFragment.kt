/*
 *   Nextcloud Android client application
 *
 *   @author Kilian Périsset
 *   Copyright (C) 2020 Infomaniak Network SA
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License (GPLv3),
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DisplayUtils

/**
 * Dialog requiring confirmation when a file/folder is too "big" to be synchronized/downloaded on device.
 */
class SyncFileNotEnoughSpaceDialogFragment :
    ConfirmationDialogFragment(),
    ConfirmationDialogFragmentListener {

    private var targetFile: OCFile? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        targetFile = requireArguments().getParcelableArgument(ARG_PASSED_FILE, OCFile::class.java)
        setOnConfirmationListener(this)
        return super.onCreateDialog(savedInstanceState)
    }

    /**
     * (Only if file is a folder), will access the destination folder to allow user to choose what to synchronize
     */
    override fun onConfirmation(callerTag: String?) {
        val frag = targetFragment as OCFileListFragment?

        if (frag != null && targetFile != null) {
            frag.onItemClicked(targetFile)
        }
    }

    /**
     * Will abort/cancel the process (is neutral to "hack" android button position ._.)
     */
    override fun onNeutral(callerTag: String?) {
        // Nothing
    }

    /**
     * Will access to storage manager in order to empty useless files
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    override fun onCancel(callerTag: String?) {
        val storageIntent = Intent(StorageManager.ACTION_MANAGE_STORAGE)
        startActivityForResult(storageIntent, REQUEST_CODE_STORAGE)
    }

    companion object {
        private const val ARG_PASSED_FILE = "fragment_parent_caller"
        private const val REQUEST_CODE_STORAGE = 20

        @JvmStatic
        fun newInstance(file: OCFile, availableDeviceSpace: Long): SyncFileNotEnoughSpaceDialogFragment {
            val args = Bundle()
            val frag = SyncFileNotEnoughSpaceDialogFragment()
            val properFileSize = DisplayUtils.bytesToHumanReadable(file.fileLength)
            val properDiskAvailableSpace = DisplayUtils.bytesToHumanReadable(availableDeviceSpace)

            // Defining title, message and resources
            args.putInt(ARG_TITLE_ID, R.string.sync_not_enough_space_dialog_title)
            args.putInt(ARG_MESSAGE_RESOURCE_ID, R.string.sync_not_enough_space_dialog_placeholder)
            args.putStringArray(
                ARG_MESSAGE_ARGUMENTS,
                arrayOf(
                    file.fileName,
                    properFileSize,
                    properDiskAvailableSpace
                )
            )
            args.putParcelable(ARG_PASSED_FILE, file)

            // Defining buttons
            if (file.isFolder) {
                args.putInt(ARG_POSITIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_choose)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                args.putInt(ARG_NEGATIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_free_space)
            }
            args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_cancel)

            frag.arguments = args
            return frag
        }
    }
}
