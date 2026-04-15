/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList.helper

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload

sealed class ConflictHandlingResult {
    data object CannotCheckConflict : ConflictHandlingResult()
    data object ConflictNotExistsSameFile : ConflictHandlingResult()
    data object ConflictNotExistsRemoteFileNotFound : ConflictHandlingResult()
    data class ShowConflictResolveDialog(val file: OCFile, val upload: OCUpload) : ConflictHandlingResult()
}
