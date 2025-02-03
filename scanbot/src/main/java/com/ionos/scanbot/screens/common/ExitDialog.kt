/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.common

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ionos.scanbot.R
import com.ionos.scanbot.util.context.isActivityFinishing
import javax.inject.Inject

internal class ExitDialog @Inject constructor() {

	fun show(context: Context, onConfirmed: () -> Unit, onDenied: () -> Unit = {}) {
		if (!context.isActivityFinishing()) {
            createDialog(context, onConfirmed, onDenied)
                .also { it.show() }
		}
	}

    private fun createDialog(context: Context, onConfirmed: () -> Unit, onDenied: () -> Unit): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.scanbot_exit_confirmation_title)
            .setMessage(R.string.scanbot_exit_confirmation_message)
            .setPositiveButton(R.string.scanbot_ok_btn_title) { dialog, _ ->
                dialog.dismiss()
                onConfirmed()
            }
            .setNegativeButton(R.string.scanbot_cancel_btn_title) { dialog, _ ->
                dialog.dismiss()
                onDenied()
            }
            .create()
    }
}
