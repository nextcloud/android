/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.files

import com.owncloud.android.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

sealed class FileIndicator(val iconRes: Int?) {
    data object Idle : FileIndicator(null)
    data object Downloading : FileIndicator(R.drawable.ic_synchronizing)
    data object Error : FileIndicator(R.drawable.ic_synchronizing_error)
    data object Downloaded : FileIndicator(R.drawable.ic_synced)
}

object FileIndicatorManager {
    private val _activeTransfers = MutableStateFlow<Map<Long, FileIndicator>>(emptyMap())
    val activeTransfers: StateFlow<Map<Long, FileIndicator>> = _activeTransfers

    fun update(fileId: Long, status: FileIndicator) {
        _activeTransfers.update { current ->
            if (status is FileIndicator.Idle) {
                current - fileId
            } else {
                current + (fileId to status)
            }
        }
    }
}
