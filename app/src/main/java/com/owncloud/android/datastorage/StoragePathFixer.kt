/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datastorage

import android.os.Environment
import com.nextcloud.client.preferences.AppPreferences

class StoragePathFixer(private val preferences: AppPreferences) {

    fun fixIfNeeded() {
        if (preferences.isStoragePathFixEnabled) {
            return
        }

        if (preferences.getStoragePath("").isNullOrEmpty()) {
            applyDefaultStoragePath()
        } else {
            preferences.removeKeysMigrationPreference()
        }

        preferences.isStoragePathFixEnabled = true
    }

    private fun applyDefaultStoragePath() {
        if (preferences.lastSeenVersionCode != 0) {
            saveStoragePath(Environment.getExternalStorageDirectory().absolutePath)
            return
        }

        findIndexableStoragePoint()?.let { saveStoragePath(it.path) }
    }

    private fun findIndexableStoragePoint(): StoragePoint? {
        val publicStoragePoints = DataStorageProvider.getInstance()
            .availableStoragePoints
            .filter { it.privacyType == StoragePoint.PrivacyType.PUBLIC }

        return publicStoragePoints.firstOrNull { it.storageType == StoragePoint.StorageType.INTERNAL }
            ?: publicStoragePoints.firstOrNull()
    }

    private fun saveStoragePath(path: String?) {
        preferences.setStoragePath(path)
        preferences.removeKeysMigrationPreference()
    }
}
