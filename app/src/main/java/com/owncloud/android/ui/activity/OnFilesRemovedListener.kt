/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.owncloud.android.datamodel.OCFile

interface OnFilesRemovedListener {
    fun onFilesRemoved()
    fun onAutoUploadFolderRemoved(
        entities: List<SyncedFolderEntity>,
        filesToRemove: List<OCFile>,
        onlyLocalCopy: Boolean
    )
}
