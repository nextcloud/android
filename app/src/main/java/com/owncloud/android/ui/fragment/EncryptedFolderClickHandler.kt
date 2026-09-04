/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.utils.e2ee.model.E2EEAction
import com.nextcloud.utils.e2ee.model.E2EEKeyCheck
import com.nextcloud.utils.extensions.showEncryptionDialog
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.launch

class EncryptedFolderClickHandler(private val fragment: OCFileListFragment) {

    companion object {
        private const val TAG = "EncryptedFolderClickHandler"
    }

    private var checkingKeysSnackbar: Snackbar? = null

    /**
     * Executed when user taps the 'New encrypted folder' action.
     */
    fun onNewEncryptedFolder() {
        checkingKeysSnackbar = DisplayUtils.createAndShowSnackMessage(
            fragment,
            R.string.encryption_key_checking_keys
        )

        fragment.lifecycleScope.launch {
            val state = fragment.e2eeActionResolver.checkKeys()
            dismissCheckingSnackbar()
            when (state) {
                E2EEKeyCheck.NO_NETWORK, E2EEKeyCheck.CHECK_FAILED, E2EEKeyCheck.E2EE_UNAVAILABLE -> {
                    state.getMessageId(E2EEAction.NEW_FOLDER)?.let { DisplayUtils.showSnackMessage(fragment, it) }
                }

                E2EEKeyCheck.ONLY_ON_SERVER, E2EEKeyCheck.MISSING_EVERYWHERE -> {
                    Log_OC.d(TAG, "keys found on server but missing locally, redirecting to encryption setup")
                    fragment.showEncryptionDialog(OCFile.ROOT_PATH)
                }

                E2EEKeyCheck.SAME_AS_SERVER -> {
                    Log_OC.d(TAG, "keys valid, proceeding with new encrypted folder creation")
                    fragment.showCreateFolderDialog(true)
                }

                E2EEKeyCheck.ONLY_ON_DEVICE, E2EEKeyCheck.DIFFERS_FROM_SERVER -> {
                    state.getDialog(E2EEAction.NEW_FOLDER)?.let { fragment.e2eeDialogPresenter.show(it) }
                }
            }
        }
    }

    fun onEncryptedFolderClick(file: OCFile, position: Int) {
        val fileActivity = fragment.activity as? FileActivity ?: run {
            Log_OC.e(TAG, "activity is not file activity, cannot handle encrypted folder")
            return
        }

        checkingKeysSnackbar = DisplayUtils.createAndShowSnackMessage(
            fragment,
            R.string.encryption_key_checking_keys
        )

        fragment.run {
            lifecycleScope.launch {
                val state = e2eeActionResolver.checkKeys()

                dismissCheckingSnackbar()

                when (state) {
                    E2EEKeyCheck.NO_NETWORK, E2EEKeyCheck.CHECK_FAILED, E2EEKeyCheck.E2EE_UNAVAILABLE -> {
                        state.getMessageId(E2EEAction.OPEN)?.let { DisplayUtils.showSnackMessage(fragment, it) }
                    }

                    E2EEKeyCheck.ONLY_ON_SERVER -> {
                        fragment.showEncryptionDialog(file.remotePath)
                    }

                    E2EEKeyCheck.MISSING_EVERYWHERE -> {
                        e2eeActionResolver.markFolderReadOnly(file)
                        state.getMessageId(E2EEAction.OPEN)?.let { DisplayUtils.showSnackMessage(fragment, it) }
                    }

                    E2EEKeyCheck.ONLY_ON_DEVICE, E2EEKeyCheck.DIFFERS_FROM_SERVER -> {
                        e2eeActionResolver.markFolderReadOnly(file)
                        state.getDialog(E2EEAction.OPEN)?.let { e2eeDialogPresenter.show(it) }
                    }

                    E2EEKeyCheck.SAME_AS_SERVER -> {
                        if (e2eeActionResolver.checkFolderMetadataKey(file)) {
                            onFolderKeyVerified(file, position, fileActivity)
                        } else {
                            DisplayUtils.showSnackMessage(
                                fragment,
                                R.string.encryption_open_key_mismatch
                            )
                        }
                    }
                }
            }
        }
    }

    private fun dismissCheckingSnackbar() {
        DisplayUtils.dismissSnackMessage(checkingKeysSnackbar)
        checkingKeysSnackbar = null
    }

    private fun onFolderKeyVerified(file: OCFile, position: Int, fileActivity: FileActivity) {
        val user = fileActivity.user.orElseThrow { RuntimeException() }
        val capability = fragment.mContainerActivity.getStorageManager().getCapability(user.accountName)

        if (capability.endToEndEncryption.isFalse || capability.endToEndEncryption.isUnknown) {
            DisplayUtils.showSnackMessage(fragment, R.string.end_to_end_encryption_not_enabled)
            return
        }

        if (FileOperationsHelper.isEndToEndEncryptionSetup(fragment.context, user)) {
            onEncryptionSetupComplete(file, position)
        } else {
            fragment.showEncryptionDialog(file.remotePath)
        }
    }

    private fun onEncryptionSetupComplete(file: OCFile?, position: Int) {
        fragment.searchFragment = false
        fragment.mHideFab = false

        val folderPickerActivity = fragment.mContainerActivity as? FolderPickerActivity
        if (folderPickerActivity?.isDoNotEnterEncryptedFolder == true) {
            DisplayUtils.showSnackMessage(fragment, R.string.copy_move_to_encrypted_folder_not_supported)
        } else {
            fragment.browseToFolder(file, position)
        }
    }
}
