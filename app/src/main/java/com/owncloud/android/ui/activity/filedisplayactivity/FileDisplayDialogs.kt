/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.filedisplayactivity

import android.view.WindowManager.BadTokenException
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.TermsOfServiceDialog
import com.owncloud.android.utils.theme.ViewThemeUtils

class FileDisplayDialogs(private val activity: AppCompatActivity, private val viewThemeUtils: ViewThemeUtils) {

    fun showWrongStoragePath() {
        try {
            val builder = MaterialAlertDialogBuilder(activity, R.style.Theme_ownCloud_Dialog)
                .setTitle(R.string.wrong_storage_path)
                .setMessage(R.string.wrong_storage_path_desc)
                .setPositiveButton(R.string.dialog_close) { dialog, _ -> dialog?.dismiss() }
                .setIcon(R.drawable.ic_settings)

            viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity.applicationContext, builder)

            builder.create().show()
        } catch (e: BadTokenException) {
            Log_OC.e(TAG, "Error showing wrong storage info, so skipping it: ${e.message}")
        }
    }

    fun showSyncedFoldersMigrationInfo(onOpenAutoUpload: () -> Unit) {
        MaterialAlertDialogBuilder(activity, R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.drawer_synced_folders)
            .setMessage(R.string.synced_folders_new_info)
            .setPositiveButton(R.string.drawer_open) { dialog, _ ->
                dialog?.dismiss()
                onOpenAutoUpload()
            }
            .setNegativeButton(R.string.drawer_close) { dialog, _ -> dialog?.dismiss() }
            .setIcon(R.drawable.nav_synced_folders)
            .show()
    }

    fun showAccountChooser(users: List<User>, onUserChosen: (User) -> Unit) {
        val accountNames = users.map { it.accountName }.toTypedArray()
        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.common_choose_account)
            .setItems(accountNames) { _, which -> onUserChosen(users[which]) }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity.applicationContext, builder)

        builder.create().show()
    }

    fun showAutoUploadFolderDeleteConfirmation(onConfirm: () -> Unit) {
        val dialog = ConfirmationDialogFragment.newInstance(
            messageResId = R.string.auto_upload_delete_dialog_description,
            messageArguments = null,
            titleResId = R.string.auto_upload_delete_dialog_title,
            titleIconId = R.drawable.ic_info,
            positiveButtonTextId = R.string.common_delete,
            negativeButtonTextId = R.string.common_cancel,
            neutralButtonTextId = NO_NEUTRAL_BUTTON
        )

        dialog.setOnConfirmationListener(object : ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
            override fun onConfirmation(callerTag: String?) = onConfirm()
            override fun onNeutral(callerTag: String?) = Unit
            override fun onCancel(callerTag: String?) = Unit
        })

        if (activity.isDialogFragmentReady(dialog)) {
            dialog.show(activity.supportFragmentManager, null)
        }
    }

    fun showTermsOfServiceIfNotShown() {
        val fragmentManager = activity.supportFragmentManager
        if (fragmentManager.findFragmentByTag(TERMS_OF_SERVICE_TAG) != null) return

        TermsOfServiceDialog().show(fragmentManager, TERMS_OF_SERVICE_TAG)
    }

    companion object {
        private const val TAG = "FileDisplayDialogs"
        private const val TERMS_OF_SERVICE_TAG = "DIALOG_TAG_SHOW_TOS"
        private const val NO_NEUTRAL_BUTTON = -1
    }
}
