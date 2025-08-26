/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

fun FileEntity.toOCFile(storageManager: FileDataStorageManager): OCFile = storageManager.createFileInstance(this)

fun List<FileEntity>.toOCFiles(storageManager: FileDataStorageManager): List<OCFile> =
    map { it.toOCFile(storageManager) }
