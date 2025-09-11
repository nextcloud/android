/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

enum class SyncState {
    SYNCING,
    COMPLETED,
    FAILED,
    IDLE;

    companion object {
        fun isSynchronizing(ordinal: Int?): Boolean = SYNCING.ordinal == ordinal
        fun isFailed(ordinal: Int?): Boolean = FAILED.ordinal == ordinal
        fun isCompleted(ordinal: Int?): Boolean = COMPLETED.ordinal == ordinal

        fun fromOrdinal(ordinal: Int?): SyncState = enumValues<SyncState>().getOrNull(ordinal ?: -1) ?: IDLE
    }
}

fun SyncState?.isSynchronizing(): Boolean = this == SyncState.SYNCING
fun SyncState?.isFailed(): Boolean = this == SyncState.FAILED
fun SyncState?.isCompleted(): Boolean = this == SyncState.COMPLETED
