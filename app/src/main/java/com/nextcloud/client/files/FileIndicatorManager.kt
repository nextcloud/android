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

enum class FileIndicator {
    Idle, Syncing, Error, Synced;

    fun getIconId(): Int? {
        return when(this) {
            Idle -> null
            Syncing -> R.drawable.ic_synchronizing
            Error -> R.drawable.ic_synchronizing_error
            Synced -> R.drawable.ic_synced
        }
    }
}

object FileIndicatorManager {
    private val _activeTransfers = MutableStateFlow<Map<Long, FileIndicator>>(emptyMap())
    val activeTransfers: StateFlow<Map<Long, FileIndicator>> = _activeTransfers

    fun update(fileId: Long, status: FileIndicator) {
        _activeTransfers.update { current ->
            current + (fileId to status)
        }
    }
}
