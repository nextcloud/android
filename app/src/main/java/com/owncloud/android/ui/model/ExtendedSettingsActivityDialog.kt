/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.model

import android.app.Activity.RESULT_OK
import android.content.Intent
import com.nextcloud.ui.ChooseStorageLocationDialogFragment
import com.owncloud.android.ui.activity.ExtendedSettingsActivity
import com.owncloud.android.ui.dialog.ThemeSelectionDialog

@Suppress("MagicNumber")
enum class ExtendedSettingsActivityDialog(val tag: String, val key: String, val resultId: Int) {
    StorageLocation("choose_storage_location", "storage_selection_result", 13),
    ThemeSelection("theme_selection", "theme_selection_result", 14);

    fun showDialog(activity: ExtendedSettingsActivity) {
        activity.run {
            if (supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }

            supportFragmentManager.setFragmentResultListener(
                key,
                this
            ) { _, result ->
                setResult(
                    RESULT_OK,
                    Intent().putExtra(
                        key,
                        result.getString(key)
                    )
                )
                finish()
            }

            if (this@ExtendedSettingsActivityDialog == StorageLocation) {
                ChooseStorageLocationDialogFragment()
            } else {
                ThemeSelectionDialog()
            }.run {
                show(supportFragmentManager, tag)
            }
        }
    }
}
