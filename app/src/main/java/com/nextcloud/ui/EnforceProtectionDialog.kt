/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R

class EnforceProtectionDialog(private val context: Context) {
    fun showDialog() {
        val options = arrayOf(
            context.getString(R.string.prefs_lock_using_passcode),
            context.getString(R.string.prefs_lock_using_device_credentials)
        )
        var selectedOptionIndex = 1

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.prefs_lock_title))
            .setCancelable(false)
            .setSingleChoiceItems(options, selectedOptionIndex) { _, which ->
                selectedOptionIndex = which
            }
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}
