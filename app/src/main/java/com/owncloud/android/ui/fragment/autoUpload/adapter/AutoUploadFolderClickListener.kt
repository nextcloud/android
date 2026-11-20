/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.adapter

import com.owncloud.android.datamodel.SyncedFolderDisplayItem

interface AutoUploadFolderClickListener {
    fun onSyncStatusToggleClick(item: SyncedFolderDisplayItem)
    fun onSyncFolderSettingsClick(item: SyncedFolderDisplayItem)
    fun onSyncFolderDeleteUploadedClick(item: SyncedFolderDisplayItem)
    fun onVisibilityToggleClick(item: SyncedFolderDisplayItem)
    fun showSubFolderWarningDialog()
}
