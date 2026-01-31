/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.nextcloud.client.database.entity.UploadEntity
import com.owncloud.android.db.OCUpload

sealed class AutoUploadEntityResult {
    data object NonRetryable : AutoUploadEntityResult()
    data object CreationError : AutoUploadEntityResult()
    data object Uploaded : AutoUploadEntityResult()
    data class Success(val data: Pair<UploadEntity, OCUpload>) : AutoUploadEntityResult()
}
