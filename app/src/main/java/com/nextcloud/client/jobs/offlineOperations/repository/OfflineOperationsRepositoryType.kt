/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations.repository

import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.owncloud.android.datamodel.OCFile

interface OfflineOperationsRepositoryType {
    fun getAllSubEntities(fileId: Long): List<OfflineOperationEntity>
    fun deleteOperation(file: OCFile)
    fun updateNextOperations(operation: OfflineOperationEntity)
    fun convertToOCFiles(fileId: Long): List<OCFile>
}
