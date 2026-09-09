/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nextcloud.utils.e2ee.model.E2EEAction
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.ARG_ACTION
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.ARG_FILE_PATH
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.RESULT_REQUEST_KEY
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.SUCCESS
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.launch

private const val TAG = "OCFileListFragmentExtensions"

fun OCFileListFragment.showEncryptionDialog(remotePath: String?, action: E2EEAction) {
    if (parentFragmentManager.findFragmentByTag(SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG) != null) {
        return
    }

    val user = accountManager.user
    val connectivityService = typedActivity<FileActivity>()?.connectivityService
    connectivityService?.isNetworkAndServerAvailable { result ->
        if (result) {
            SetupEncryptionDialogFragment.newInstance(user, remotePath, action)
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

        val action = bundle.getSerializableArgument(ARG_ACTION, E2EEAction::class.java)
        if (action == null) {
            Log_OC.e(TAG, "no pending encryption action, nothing to continue with")
            return@setFragmentResultListener
        }

        when (action) {
            E2EEAction.NEW_FOLDER -> createFolder(true)

            E2EEAction.OPEN -> folderFromResult(bundle)?.let { clickHandler.openAfterKeySetup(it) }

            E2EEAction.ENCRYPT -> folderFromResult(bundle)?.let { file ->
                lifecycleScope.launch {
                    folderEncryption.toggle(file.toEncryptionEvent(true))
                }
            }
        }
    }
}

private fun OCFileListFragment.folderFromResult(bundle: Bundle): OCFile? {
    val remotePath = bundle.getString(ARG_FILE_PATH, null)
    if (remotePath == null) {
        Log_OC.e(TAG, "file path is null")
        return null
    }

    val file = mContainerActivity.storageManager.getFileByEncryptedRemotePath(remotePath)
    if (file == null) {
        Log_OC.e(TAG, "file is null, cannot continue encryption action")
    }

    return file
}
