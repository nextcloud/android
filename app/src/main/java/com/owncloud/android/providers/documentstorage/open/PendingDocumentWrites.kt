/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.open

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracked per write, not per document, because a document can be open for writing more than once at the same time.
 */
internal class PendingDocumentWrites {

    private val writes: MutableSet<PendingDocumentWrite> = ConcurrentHashMap.newKeySet()

    fun begin(fileId: Long, accountName: String): PendingDocumentWrite =
        PendingDocumentWrite(fileId, accountName).also(writes::add)

    fun closed(write: PendingDocumentWrite) {
        write.state = PendingWriteState.CLOSED_UPLOAD_NOT_QUEUED
    }

    fun end(write: PendingDocumentWrite) {
        writes.remove(write)
    }

    fun states(isAffected: (PendingDocumentWrite) -> Boolean): Set<PendingWriteState> =
        writes.filter(isAffected).mapTo(mutableSetOf()) { it.state }
}
