/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import com.nextcloud.utils.extensions.setVisibleIf

@Suppress("DEPRECATION")
class ListPreferenceDialog(context: Context?, attrs: AttributeSet?) : ListPreference(context, attrs) {

    fun showDialog() {
        if (!isDialogCreated()) {
            onClick()
        }
    }

    fun dismissible(value: Boolean) {
        if (isDialogCreated()) {
            dialog.setCancelable(value)
            dialog.setCanceledOnTouchOutside(value)
        }
    }

    fun enableCancelButton(value: Boolean) {
        if (isDialogCreated()) {
            (dialog as? AlertDialog)?.let {
                val cancelButton = it.getButton(Dialog.BUTTON_NEGATIVE)
                cancelButton?.setVisibleIf(value)
                cancelButton?.isEnabled = value
            }
        }
    }

    private fun isDialogCreated(): Boolean = dialog != null
}
