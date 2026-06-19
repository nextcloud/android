/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.tags.repository

import com.nextcloud.repository.ClientRepository
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.tags.CreateTagRemoteOperation
import com.owncloud.android.lib.resources.tags.DeleteTagRemoteOperation
import com.owncloud.android.lib.resources.tags.GetTagsRemoteOperation
import com.owncloud.android.lib.resources.tags.PutTagRemoteOperation
import com.owncloud.android.lib.resources.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TagManagementRepositoryImpl(private val clientRepository: ClientRepository) : TagManagementRepository {

    companion object {
        private const val TAG = "TagManagementRepositoryImpl"
    }

    override suspend fun fetch(
        fileId: Long,
        currentTags: List<Tag>
    ): List<Tag> = withContext(Dispatchers.IO) {
        return@withContext try {
            val ocClient = clientRepository.getOwncloudClient() ?: return@withContext listOf()
            val result = GetTagsRemoteOperation().execute(ocClient)
            if (result.isSuccess) result.resultData else listOf()
        } catch (e: Exception) {
            Log_OC.e(TAG, "cannot fetch tags: $e")
            listOf()
        }
    }

    override suspend fun assignTag(fileId: Long, tag: Tag): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val ncClient = clientRepository.getNextcloudClient() ?: return@withContext false
            val result = PutTagRemoteOperation(tag.id, fileId).execute(ncClient)
            result.isSuccess
        } catch (e: Exception) {
            Log_OC.e(TAG, "cannot assign tag: $e")
            false
        }
    }

    override suspend fun unassignTag(fileId: Long, tag: Tag): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val ncClient = clientRepository.getNextcloudClient() ?: return@withContext false
            val result = DeleteTagRemoteOperation(tag.id, fileId).execute(ncClient)
            result.isSuccess
        } catch (e: Exception) {
            Log_OC.e(TAG, "cannot unassign tag: $e")
            false
        }
    }

    override suspend fun createAndAssignTag(
        fileId: Long,
        name: String
    ): Pair<List<Tag>, String>? = withContext(Dispatchers.IO) {
        return@withContext try {
            val ncClient = clientRepository.getNextcloudClient() ?: return@withContext null
            val ocClient = clientRepository.getOwncloudClient() ?: return@withContext null

            val createResult = CreateTagRemoteOperation(name).execute(ncClient)
            if (!createResult.isSuccess) {
                return@withContext null
            }

            val tagsResult = GetTagsRemoteOperation().execute(ocClient)
            if (!tagsResult.isSuccess) {
                return@withContext null
            }

            val allTags = tagsResult.resultData
            val newTag = allTags.find { it.name == name } ?: return@withContext null

            val result = PutTagRemoteOperation(newTag.id, fileId).execute(ncClient)
            if (result.isSuccess) allTags to newTag.id else null
        } catch (e: Exception) {
            Log_OC.e(TAG, "cannot create and assign tag: $e")
            null
        }
    }
}
