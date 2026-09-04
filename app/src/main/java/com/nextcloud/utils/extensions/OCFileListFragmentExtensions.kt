/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.ARG_FILE_PATH
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.RESULT_REQUEST_KEY
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.SUCCESS
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.launch

private const val TAG = "OCFileListFragmentExtensions"

fun OCFileListFragment.showEncryptionDialog(remotePath: String?) {
    if (parentFragmentManager.findFragmentByTag(SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG) != null) {
        return
    }

    val user = accountManager.user
    val connectivityService = typedActivity<FileActivity>()?.connectivityService
    connectivityService?.isNetworkAndServerAvailable { result ->
        if (result == true) {
            SetupEncryptionDialogFragment.newInstance(user, remotePath)
                .show(parentFragmentManager, SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG)
            return@isNetworkAndServerAvailable
        }

        DisplayUtils.showSnackMessage(
            this,
            R.string.internet_connection_required_for_encrypted_folder_setup
        )
    }
}

fun OCFileListFragment.listenEncryptionDialogResult() {
    parentFragmentManager.setFragmentResultListener(
        RESULT_REQUEST_KEY,
        this
    ) { _: String?, bundle: Bundle ->
        val result = bundle.getBoolean(SUCCESS, false)
        if (!result) {
            Log_OC.d(TAG, "setup encryption dialog is dismissed")
            return@setFragmentResultListener
        }

        val fileRemotePath = bundle.getString(ARG_FILE_PATH, null)
        if (fileRemotePath == null) {
            Log_OC.e(TAG, "file path is null")
            return@setFragmentResultListener
        }

        val file: OCFile? = mContainerActivity.getStorageManager().getFileByDecryptedRemotePath(fileRemotePath)
        if (file == null) {
            Log_OC.e(TAG, "file is null, cannot toggle encryption")
            return@setFragmentResultListener
        }

        if (file.isRootDirectory) {
            Log_OC.d(
                TAG,
                "result of setup encryption triggered in root directory, this call is for " +
                    "creating encrypted folder"
            )
            createFolder(true)
            return@setFragmentResultListener
        }

        lifecycleScope.launch {
            folderEncryption.toggle(file.toEncryptionEvent(true))
        }
    }
}
