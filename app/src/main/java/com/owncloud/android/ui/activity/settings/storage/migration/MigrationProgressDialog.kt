/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogStorageMigrationProgressBinding
import com.owncloud.android.ui.dialog.extensions.themeButtons
import com.owncloud.android.utils.theme.ViewThemeUtils

class MigrationProgressDialog(activity: Activity, private val viewThemeUtils: ViewThemeUtils) {

    private val binding = DialogStorageMigrationProgressBinding.inflate(activity.layoutInflater)
    private val dialog: AlertDialog

    init {
        viewThemeUtils.material.colorProgressBar(binding.storageMigrationProgress)
        binding.storageMigrationMessage.setText(R.string.file_migration_preparing)

        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.file_migration_dialog_title)
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_close) { dialog, _ -> dialog.dismiss() }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder)
        dialog = builder.create()
    }

    fun show() {
        dialog.show()
        dialog.themeButtons(viewThemeUtils)
        closeButton().isVisible = false
    }

    fun setMessage(@StringRes messageResId: Int) {
        binding.storageMigrationMessage.setText(messageResId)
    }

    fun showFailure(@StringRes messageResId: Int) {
        setMessage(messageResId)
        binding.storageMigrationProgress.isVisible = false
        binding.storageMigrationFailedIcon.isVisible = true
        closeButton().isVisible = true
    }

    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun closeButton() = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
}
