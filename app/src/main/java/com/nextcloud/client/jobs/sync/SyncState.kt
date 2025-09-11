/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

enum class SyncState {
    SYNCING, COMPLETED, FAILED, IDLE;

    companion object {
        fun fromOrdinal(ordinal: Int?): SyncState =
            enumValues<SyncState>().getOrNull(ordinal ?: -1) ?: IDLE
    }
}
