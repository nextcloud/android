/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable

interface AutoUploadFolderConfigurationListener {
    fun onSaveAutoUploadFolderConfiguration(syncedFolder: SyncedFolderParcelable)
    fun onCancelAutoUploadFolderConfiguration()
    fun onDeleteAutoUploadFolderConfiguration(syncedFolder: SyncedFolderParcelable)
    fun onRemoteFolderPickerRequested()
    fun onLocalFolderPickerRequested()
}
