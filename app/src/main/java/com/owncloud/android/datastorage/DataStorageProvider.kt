/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datastorage

import android.os.Environment
import com.nextcloud.utils.extensions.canonicalPathOrAbsolute
import com.owncloud.android.MainApp
import com.owncloud.android.datastorage.StoragePoint.PrivacyType
import com.owncloud.android.datastorage.StoragePoint.StorageType
import java.io.File

class DataStorageProvider private constructor() {

    val availableStoragePoints: Array<StoragePoint>
        get() {
            val context = MainApp.getAppContext()
            val storagePoints = UniqueStorageList()

            context.externalMediaDirs.forEach { addSharedStoragePoint(storagePoints, it, PrivacyType.PUBLIC) }
            storagePoints.add(createStoragePoint(context.filesDir, StorageType.INTERNAL, PrivacyType.PRIVATE))
            context.getExternalFilesDirs(null).forEach { addSharedStoragePoint(storagePoints, it, PrivacyType.PRIVATE) }

            return storagePoints.toTypedArray()
        }

    fun findStoragePoint(path: String): StoragePoint? {
        val canonicalPath = File(path).canonicalPathOrAbsolute
        val knownPoint = availableStoragePoints.firstOrNull { File(it.path).canonicalPathOrAbsolute == canonicalPath }
            ?: return classifyStoragePath(path, canonicalPath)

        return StoragePoint(path, path, knownPoint.storageType, knownPoint.privacyType)
    }

    fun isSameLocation(firstPath: String, secondPath: String): Boolean =
        File(firstPath).canonicalPathOrAbsolute == File(secondPath).canonicalPathOrAbsolute

    fun allowAccessFromOtherApps(): Boolean {
        val storagePath = MainApp.getStoragePath()

        return availableStoragePoints.any { it.path == storagePath && it.privacyType == PrivacyType.PUBLIC }
    }

    private fun addSharedStoragePoint(storagePoints: UniqueStorageList, dir: File?, privacyType: PrivacyType) {
        if (dir == null || !isMounted(dir)) return

        storagePoints.add(createStoragePoint(dir, resolveStorageType(dir), privacyType))
    }

    private fun classifyStoragePath(path: String, canonicalPath: String): StoragePoint? {
        val file = File(path)
        val dataDir = MainApp.getAppContext().dataDir.canonicalPathOrAbsolute

        if (canonicalPath == dataDir || canonicalPath.startsWith(dataDir + File.separator)) {
            return createStoragePoint(file, StorageType.INTERNAL, PrivacyType.PRIVATE)
        }

        if (!isMounted(file)) return null

        val privacyType = if (canonicalPath.contains(APP_SPECIFIC_DATA_DIR)) PrivacyType.PRIVATE else PrivacyType.PUBLIC

        return createStoragePoint(file, resolveStorageType(file), privacyType)
    }

    private fun isMounted(dir: File): Boolean = Environment.getExternalStorageState(dir) == Environment.MEDIA_MOUNTED

    private fun resolveStorageType(dir: File): StorageType = try {
        if (Environment.isExternalStorageRemovable(dir)) StorageType.EXTERNAL else StorageType.INTERNAL
    } catch (e: IllegalArgumentException) {
        StorageType.INTERNAL
    }

    private fun createStoragePoint(dir: File, storageType: StorageType, privacyType: PrivacyType): StoragePoint {
        val path = dir.absolutePath

        return StoragePoint(path, path, storageType, privacyType)
    }

    companion object {
        private const val APP_SPECIFIC_DATA_DIR = "/Android/data/"

        val instance = DataStorageProvider()
    }
}
