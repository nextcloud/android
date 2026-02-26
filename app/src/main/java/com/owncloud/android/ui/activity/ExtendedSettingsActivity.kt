/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog

class ExtendedSettingsActivity : AppCompatActivity() {

    @Suppress("ReturnCount")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dialogKey = intent.getStringExtra(EXTRA_DIALOG_TYPE) ?: run {
            finish()
            return
        }

        val dialogType = ExtendedSettingsActivityDialog.entries.find { it.key == dialogKey } ?: run {
            finish()
            return
        }

        val dismissable = intent.getBooleanExtra(EXTRA_DISMISSABLE, true)
        dialogType.showDialog(this, dismissable)
    }

    companion object {
        private const val EXTRA_DISMISSABLE = "dismissable"
        private const val EXTRA_DIALOG_TYPE = "dialog_type"

        @JvmOverloads
        fun createIntent(
            context: Context,
            dialogType: ExtendedSettingsActivityDialog,
            dismissable: Boolean = true
        ): Intent = Intent(context, ExtendedSettingsActivity::class.java).apply {
            putExtra(EXTRA_DIALOG_TYPE, dialogType.key)
            putExtra(EXTRA_DISMISSABLE, dismissable)
        }
    }
}
