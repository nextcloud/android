/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import com.owncloud.android.ui.dialog.parcel.AutoUploadFolderParcelable

interface AutoUploadFolderConfigurationListener {
    fun onSaveAutoUploadFolderConfiguration(syncedFolder: AutoUploadFolderParcelable)
    fun onCancelAutoUploadFolderConfiguration()
    fun onDeleteAutoUploadFolderConfiguration(syncedFolder: AutoUploadFolderParcelable)
    fun onRemoteFolderPickerRequested()
    fun onLocalFolderPickerRequested()
}
