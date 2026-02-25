/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import com.nextcloud.client.account.User
import com.nextcloud.client.database.dao.SyncedFolderDao
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

object SyncedFolderObserver {

    private val entities = CopyOnWriteArraySet<SyncedFolderEntity>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(dao: SyncedFolderDao) {
        if (job?.isActive == true) return

        job = scope.launch {
            dao.getAllAsFlow()
                .distinctUntilChanged()
                .collect { updatedEntities ->
                    entities.clear()
                    entities.addAll(updatedEntities)
                }
        }
    }

    fun isAutoUploadFolder(file: ServerFileInterface, user: User): Boolean {
        val normalizedRemotePath = file.remotePath
            .trimEnd('/')

        return entities
            .stream()
            .filter { it.account == user.accountName }
            .filter { it.remotePath != null }
            .anyMatch { entity ->
                val entityRemotePath = entity.remotePath?.trimEnd('/') ?: return@anyMatch false
                normalizedRemotePath.contains(entityRemotePath)
            }
    }
}
