/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration.model

interface StorageMigrationProgressListener {
    fun onStorageMigrationFinished(storagePath: String, succeed: Boolean)
    fun onCancelMigration()
}
