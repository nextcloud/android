/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.filesRepository

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.android.lib.resources.recommendations.GetRecommendationsRemoteOperation
import com.nextcloud.android.lib.richWorkspace.RichWorkspaceDirectEditingRemoteOperation
import com.nextcloud.client.database.entity.RecommendedFileEntity
import com.nextcloud.client.database.entity.toEntity
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooGenericExceptionCaught")
class RemoteFilesRepository(private val clientRepository: ClientRepository, lifecycleOwner: LifecycleOwner) :
    FilesRepository {
    private val tag = "FilesRepository"
    private val scope = lifecycleOwner.lifecycleScope

    override fun fetchRecommendedFiles(
        ignoreETag: Boolean,
        storageManager: FileDataStorageManager,
        onCompleted: (ArrayList<RecommendedFileEntity>) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val cachedRecommendations = storageManager.recommendedFileDao.getAll()
            if (cachedRecommendations.isNotEmpty() && !ignoreETag) {
                Log_OC.d(tag, "Returning cached recommendations.")
                withContext(Dispatchers.Main) {
                    onCompleted(ArrayList(cachedRecommendations))
                }
                return@launch
            }

            try {
                val client = clientRepository.getNextcloudClient()
                if (client == null) {
                    Log_OC.w(tag, "No Nextcloud client available. Returning cached recommendations.")
                    withContext(Dispatchers.Main) { onCompleted(ArrayList(cachedRecommendations)) }
                    return@launch
                }

                val result = GetRecommendationsRemoteOperation().execute(client)
                if (result.isSuccess) {
                    val recommendations = result.getResultData().recommendations
                    Log_OC.d(tag, "Fetched ${recommendations.size} recommended files from remote.")

                    val recommendationsEntity = recommendations.toEntity()
                    storageManager.recommendedFileDao.insertAll(recommendationsEntity)

                    withContext(Dispatchers.Main) {
                        onCompleted(ArrayList(recommendationsEntity))
                    }
                } else {
                    Log_OC.w(tag, "Failed to fetch recommended files (code=${result.code}). " +
                        "Using cached values.")
                    withContext(Dispatchers.Main) { onCompleted(ArrayList(cachedRecommendations)) }
                }
            } catch (e: Exception) {
                Log_OC.e(tag, "Error fetching recommended files. Returning cached values.", e)
                withContext(Dispatchers.Main) { onCompleted(ArrayList(cachedRecommendations)) }
            }
        }
    }

    override fun createRichWorkspace(remotePath: String, onCompleted: (String) -> Unit, onError: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = clientRepository.getOwncloudClient() ?: return@launch
                val url = RichWorkspaceDirectEditingRemoteOperation(remotePath)
                    .execute(client)
                    .takeIf { it.isSuccess }
                    ?.singleData as? String

                withContext(Dispatchers.Main) {
                    url?.let(onCompleted) ?: onError()
                }
            } catch (e: Exception) {
                Log_OC.e(tag, "Exception caught while creating rich workspace: $e")
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }
}
