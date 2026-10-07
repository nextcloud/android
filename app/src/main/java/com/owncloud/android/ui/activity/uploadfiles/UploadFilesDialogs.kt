/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.uploadfiles

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.owncloud.android.R
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog
import com.owncloud.android.ui.dialog.LocalStoragePathPickerDialogFragment

class UploadFilesDialogs(private val activity: AppCompatActivity) {

    private var waitDialog: DialogFragment? = null
    private var storagePathPickerDialog: LocalStoragePathPickerDialogFragment? = null

    fun showStoragePathPicker() {
        val transaction = activity.supportFragmentManager.beginTransaction().addToBackStack(null)
        storagePathPickerDialog = LocalStoragePathPickerDialogFragment.newInstance().also {
            it.show(transaction, LocalStoragePathPickerDialogFragment.LOCAL_STORAGE_PATH_PICKER_FRAGMENT)
        }
    }

    fun dismissStoragePathPicker() {
        storagePathPickerDialog?.dismissAllowingStateLoss()
    }

    fun showWaitDialog() {
        waitDialog = IndeterminateProgressDialog.newInstance(R.string.wait_a_moment, false).also {
            it.show(activity.supportFragmentManager, WAIT_DIALOG_TAG)
        }
    }

    fun dismissWaitDialog() {
        val dialog = waitDialog ?: return
        if (!activity.isDialogFragmentReady(dialog)) return

        dialog.dismiss()
        waitDialog = null
    }

    fun showQueryToMoveDialog(listener: ConfirmationDialogFragmentListener) {
        val dialog = ConfirmationDialogFragment.newInstance(
            messageResId = R.string.upload_query_move_foreign_files,
            messageArguments = arrayOf(activity.getString(R.string.app_name)),
            titleResId = NO_TITLE,
            positiveButtonTextId = R.string.common_yes,
            negativeButtonTextId = R.string.common_no,
            neutralButtonTextId = NO_BUTTON
        )
        dialog.setOnConfirmationListener(listener)
        dialog.show(activity.supportFragmentManager, QUERY_TO_MOVE_DIALOG_TAG)
    }

    fun showSubFolderWarningDialog(onConfirmed: () -> Unit) {
        val dialog = ConfirmationDialogFragment.newInstance(
            messageResId = R.string.auto_upload_sub_folder_warning,
            messageArguments = null,
            titleResId = R.string.sync_duplication,
            titleIconId = R.drawable.ic_info,
            positiveButtonTextId = R.string.sync_anyway,
            negativeButtonTextId = R.string.common_cancel,
            neutralButtonTextId = NO_BUTTON
        )

        dialog.setOnConfirmationListener(object : ConfirmationDialogFragmentListener {
            override fun onConfirmation(callerTag: String?) = onConfirmed()
            override fun onNeutral(callerTag: String?) = Unit
            override fun onCancel(callerTag: String?) = Unit
        })

        if (activity.isDialogFragmentReady(dialog)) {
            dialog.show(activity.supportFragmentManager, SUB_FOLDER_WARNING_DIALOG_TAG)
        }
    }

    companion object {
        const val QUERY_TO_MOVE_DIALOG_TAG = "QUERY_TO_MOVE"
        private const val SUB_FOLDER_WARNING_DIALOG_TAG = "SUB_FOLDER_WARNING_DIALOG"
        private const val WAIT_DIALOG_TAG = "WAIT"
        private const val NO_TITLE = 0
        private const val NO_BUTTON = -1
    }
}
