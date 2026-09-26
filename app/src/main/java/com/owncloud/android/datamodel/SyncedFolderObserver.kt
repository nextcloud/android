/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import com.nextcloud.client.account.User
import com.nextcloud.client.database.dao.SyncedFolderDao
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object SyncedFolderObserver {

    @Volatile
    private var syncedFoldersMap = mapOf<String, Set<String>>()

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(dao: SyncedFolderDao) {
        if (job?.isActive == true) return

        job = scope.launch {
            dao.getAllAsFlow()
                .distinctUntilChanged()
                .collect { updatedEntities ->
                    syncedFoldersMap = updatedEntities
                        .filter { it.remotePath != null && it.account != null }
                        .groupBy { it.account!! }
                        .mapValues { (_, entities) ->
                            entities.map { it.remotePath!!.trimEnd('/') }.toSet()
                        }
                }
        }
    }

    @Suppress("ReturnCount")
    fun isAutoUploadFolder(file: ServerFileInterface, user: User): Boolean {
        val accountFolders = syncedFoldersMap[user.accountName] ?: return false
        val normalizedRemotePath = file.remotePath.trimEnd('/')
        if (normalizedRemotePath.isEmpty()) return false
        return accountFolders.any { entityPath ->
            normalizedRemotePath == entityPath
        }
    }
}
