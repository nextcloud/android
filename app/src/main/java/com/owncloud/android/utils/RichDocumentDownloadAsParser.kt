/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.owncloud.android.ui.model.DownloadAsResult
import com.owncloud.android.ui.model.DownloadAsV1
import com.owncloud.android.ui.model.DownloadAsV2

object RichDocumentDownloadAsParser {
    fun parse(json: String?): DownloadAsResult? {
        if (json.isNullOrBlank()) return null

        var result = DownloadAsV2.tryDeserialize(json)
        if (result == null) {
            result = DownloadAsV1.tryDeserialize(json)
        }

        return result
    }
}
