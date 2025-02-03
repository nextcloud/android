/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.common

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ionos.scanbot.databinding.ScanbotLockProgressDialogBinding

class LockProgressDialog {
    private var dialog: AlertDialog? = null

    fun start(activity: Activity, @StringRes messageRes: Int) {
        showProgressDialog(activity, activity.getString(messageRes))
    }

    fun start(activity: Activity, message: String) {
        showProgressDialog(activity, message)
    }

    private fun showProgressDialog(activity: Activity, message: String) {
        if (isShowing()) {
            stop()
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            val binding = ScanbotLockProgressDialogBinding.inflate(activity.layoutInflater)

            binding.progressTextView.text = message

            dialog = MaterialAlertDialogBuilder(activity)
                .setView(binding.root)
                .setCancelable(false)
                .create()
                .also(AlertDialog::show)
        }
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }

    fun stop() {
        dialog?.dismiss()
    }
}
