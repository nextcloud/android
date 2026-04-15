/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList.helper

import com.owncloud.android.db.OCUpload

interface UploadListItemOnClick {
    fun onLastUploadResultConflictClick(upload: OCUpload)
}
