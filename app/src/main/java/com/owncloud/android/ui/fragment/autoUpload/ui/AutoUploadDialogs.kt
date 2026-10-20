/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.autoUpload.ui

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.BatteryOptimizationHelper
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.owncloud.android.R
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.extensions.themeButtons
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadDialogs(
    private val fragment: Fragment,
    private val viewThemeUtils: ViewThemeUtils,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager
) {

    fun buildPowerCheckDialog(): AlertDialog {
        val context = fragment.requireContext()
        val builder = MaterialAlertDialogBuilder(context)
            .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
            .setTitle(R.string.autoupload_disable_power_save_check)
            .setMessage(context.getString(R.string.power_save_check_dialog_message))

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(context, builder)

        return builder.create()
    }

    fun showDeleteAllUploadedDialog(user: User) {
        val dialog = ConfirmationDialogFragment.newInstance(
            R.string.autoupload_delete_uploaded_all_dialog_description,
            null,
            R.string.autoupload_delete_uploaded_all_dialog_title,
            R.drawable.selector_trashbin,
            R.string.autoupload_delete_uploaded_all_dialog_button_all_users,
            R.string.autoupload_delete_uploaded_all_dialog_button_current_user,
            R.string.common_cancel
        )
        dialog.isCancelable = false
        dialog.setOnConfirmationListener(object : ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
            override fun onConfirmation(callerTag: String?) {
                FilesSyncHelper.startLocalDeletionForEnabledSyncedFolders(syncedFolderProvider, backgroundJobManager)
            }

            override fun onCancel(callerTag: String?) {
                FilesSyncHelper.startLocalDeletionForEnabledSyncedFolders(
                    syncedFolderProvider,
                    backgroundJobManager,
                    user
                )
            }

            override fun onNeutral(callerTag: String?) = Unit
        })

        showDialog(dialog, SYNCED_FOLDER_DELETE_ALL_UPLOADED_DIALOG_TAG)
    }

    fun showDeleteUploadedDialog(syncedFolderDisplayItem: SyncedFolderDisplayItem) {
        val dialog = ConfirmationDialogFragment.newInstance(
            R.string.autoupload_delete_uploaded_dialog_description,
            null,
            R.string.autoupload_delete_uploaded_dialog_title,
            R.drawable.selector_trashbin,
            R.string.common_ok,
            R.string.common_cancel,
            -1
        )
        dialog.isCancelable = false
        dialog.setOnConfirmationListener(object : ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
            override fun onConfirmation(callerTag: String?) {
                FilesSyncHelper.startLocalDeletionForSyncedFolder(syncedFolderDisplayItem, backgroundJobManager)
            }

            override fun onNeutral(callerTag: String?) = Unit

            override fun onCancel(callerTag: String?) = Unit
        })
        showDialog(dialog, SYNCED_FOLDER_DELETE_UPLOADED_DIALOG_TAG)
    }

    fun showSubFolderWarningDialog() {
        val dialog = ConfirmationDialogFragment.newInstance(
            messageResId = R.string.auto_upload_sub_folder_warning,
            messageArguments = null,
            titleResId = R.string.sync_duplication,
            titleIconId = R.drawable.ic_info,
            positiveButtonTextId = R.string.dialog_close,
            negativeButtonTextId = -1,
            neutralButtonTextId = -1
        )

        if (isActivityReadyFor(dialog)) {
            dialog.show(fragment.childFragmentManager, SUB_FOLDER_WARNING_DIALOG_TAG)
        }
    }

    fun showDialog(dialog: DialogFragment, tag: String) {
        if (!isActivityReadyFor(dialog) || !isResumed()) {
            Log_OC.d(TAG, "SyncFolderDeleteUploaded dialog not ready")
            return
        }

        val fragmentTransaction = fragment.childFragmentManager
            .beginTransaction()
            .addToBackStack(null)
        dialog.show(fragmentTransaction, tag)
    }

    fun showBatteryOptimizationDialogIfNeeded() {
        if (!BatteryOptimizationHelper.isBatteryOptimizationEnabled(fragment.requireContext())) {
            Log_OC.d(TAG, "battery optimization is disabled")
            return
        }

        showBatteryOptimizationDialog()
    }

    private fun showBatteryOptimizationDialog() {
        if (!isResumed()) {
            Log_OC.w(TAG, "Fragment not resumed, skipping battery dialog")
            return
        }

        val context = fragment.requireContext()
        val dialog = MaterialAlertDialogBuilder(context, R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.battery_optimization_title)
            .setMessage(R.string.battery_optimization_message)
            .setPositiveButton(R.string.battery_optimization_disable) { _, _ ->
                BatteryOptimizationHelper.openBatteryOptimizationSettings(context)
            }
            .setNeutralButton(R.string.battery_optimization_close, null)
            .setIcon(R.drawable.ic_battery_alert)

        val alertDialog = dialog.show()
        alertDialog.themeButtons(viewThemeUtils)
    }

    private fun isActivityReadyFor(dialog: DialogFragment): Boolean =
        (fragment.activity as? AppCompatActivity)?.isDialogFragmentReady(dialog) == true

    private fun isResumed(): Boolean = fragment.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    companion object {
        private val TAG = AutoUploadDialogs::class.java.simpleName
        private const val SYNCED_FOLDER_DELETE_UPLOADED_DIALOG_TAG = "SYNCED_FOLDER_DELETE_UPLOADED_DIALOG_TAG"
        private const val SYNCED_FOLDER_DELETE_ALL_UPLOADED_DIALOG_TAG = "SYNCED_FOLDER_DELETE_ALL_UPLOADED_DIALOG_TAG"
        private const val SUB_FOLDER_WARNING_DIALOG_TAG = "SUB_FOLDER_WARNING_DIALOG_TAG"
    }
}
