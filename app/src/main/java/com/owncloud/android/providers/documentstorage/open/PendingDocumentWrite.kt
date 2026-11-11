/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.open

internal class PendingDocumentWrite(val fileId: Long, val accountName: String) {
    @Volatile
    var state = PendingWriteState.OPEN
}
