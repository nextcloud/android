/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.open

internal enum class PendingWriteState {
    OPEN,
    CLOSED_UPLOAD_NOT_QUEUED
}
