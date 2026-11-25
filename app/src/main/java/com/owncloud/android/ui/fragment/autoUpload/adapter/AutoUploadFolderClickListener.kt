/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.adapter

import com.owncloud.android.datamodel.AutoUploadFolder

interface AutoUploadFolderClickListener {
    fun onSyncStatusToggleClick(item: AutoUploadFolder)
    fun onSyncFolderSettingsClick(item: AutoUploadFolder)
    fun onSyncFolderDeleteUploadedClick(item: AutoUploadFolder)
    fun onVisibilityToggleClick(item: AutoUploadFolder)
    fun showSubFolderWarningDialog()
}
