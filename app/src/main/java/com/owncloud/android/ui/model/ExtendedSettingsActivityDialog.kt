/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.model

import com.nextcloud.ui.ChooseStorageLocationDialogFragment
import com.owncloud.android.ui.dialog.ThemeSelectionDialog

enum class ExtendedSettingsActivityDialog(val key: String, val resultId: Int) {
    StorageLocation(ChooseStorageLocationDialogFragment.KEY_RESULT_STORAGE_LOCATION, 13),
    Theme(ThemeSelectionDialog.RESULT_KEY, 14)
}
