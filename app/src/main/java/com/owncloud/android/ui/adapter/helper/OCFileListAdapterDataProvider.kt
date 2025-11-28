/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.datamodel.OCFile

interface OCFileListAdapterDataProvider {
    fun convertToOCFiles(id: Long): List<OCFile>
    suspend fun getFolderContent(id: Long): List<FileEntity>
    fun createFileInstance(entity: FileEntity): OCFile
}
