/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.model.OCFileFilterType
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

fun FileDataStorageManager.filter(file: OCFile, filterType: OCFileFilterType): List<OCFile> =
    if (!file.isRootDirectory) {
        getFolderContent(file, false)
    } else {
        getAllFiles().filter { ocFile ->
            when (filterType) {
                OCFileFilterType.Shared -> ocFile.isShared
                OCFileFilterType.Favorite -> ocFile.isFavorite
            }
        }
    }
