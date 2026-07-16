/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import com.nextcloud.android.lib.resources.governance.GetAllSelectableLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.GetEntityLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.HoldLabelInfo
import com.nextcloud.android.lib.resources.governance.LabelType
import com.nextcloud.android.lib.resources.governance.RemoveLabelRemoteOperation
import com.nextcloud.android.lib.resources.governance.RetentionLabelInfo
import com.nextcloud.android.lib.resources.governance.SensitivityLabelInfo
import com.nextcloud.android.lib.resources.governance.SetLabelRemoteOperation
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.fileInfo.model.CurrentEntityLabels
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.nextcloud.ui.fileInfo.model.SelectableLabels
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FileInfoRepository @Inject constructor(private val clientFactory: ClientFactory) {

    private val clientMutex = Mutex()
    private var client: NextcloudClient? = null

    suspend fun fetchAllSelectableLabels(file: OCFile, user: User): SelectableLabels {
        val empty = SelectableLabels(emptyList(), emptyList(), emptyList())
        return execute(user, empty, "Could not fetch selectable labels") { client ->
            val result = GetAllSelectableLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId).execute(client)
            if (result.isSuccess) {
                val data = result.resultData
                SelectableLabels(
                    sensitivityLabels = data?.sensitivity?.map { it.toGovernanceLabel() } ?: emptyList(),
                    retentionLabels = data?.retention?.map { it.toGovernanceLabel() } ?: emptyList(),
                    holdLabels = data?.hold?.map { it.toGovernanceLabel() } ?: emptyList()
                )
            } else {
                empty
            }
        }
    }

    suspend fun fetchEntityLabels(file: OCFile, user: User): CurrentEntityLabels =
        execute(user, CurrentEntityLabels(), "Could not fetch entity labels") { client ->
            val result = GetEntityLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId.toString()).execute(client)
            val labels = if (result.isSuccess) result.resultData else null
            CurrentEntityLabels(
                sensitivityId = labels?.sensitivity?.map { it.id }?.toSet()?.first() ?: "",
                retentionIds = labels?.retention?.map { it.id }?.toSet() ?: emptySet(),
                holdIds = labels?.hold?.map { it.id }?.toSet() ?: emptySet()
            )
        }

    suspend fun setLabel(file: OCFile, user: User, labelType: LabelType, labelId: String): Boolean =
        execute(user, false, "Could not set label") { client ->
            SetLabelRemoteOperation(ENTITY_TYPE_FILES, file.localId, labelType, labelId).execute(client).isSuccess
        }

    suspend fun removeLabel(file: OCFile, user: User, labelType: LabelType, labelId: String): Boolean =
        execute(user, false, "Could not remove label") { client ->
            RemoveLabelRemoteOperation(ENTITY_TYPE_FILES, file.localId, labelType, labelId).execute(client).isSuccess
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> execute(user: User, default: T, errorMessage: String, block: (NextcloudClient) -> T): T =
        withContext(Dispatchers.IO) {
            val client = client(user) ?: return@withContext default
            try {
                block(client)
            } catch (e: Exception) {
                Log_OC.e(TAG, errorMessage, e)
                default
            }
        }

    private suspend fun client(user: User): NextcloudClient? = clientMutex.withLock {
        client ?: runCatching { clientFactory.createNextcloudClient(user) }.getOrNull()?.also { client = it }
    }

    private fun SensitivityLabelInfo.toGovernanceLabel() = GovernanceLabel(id, name, color)
    private fun RetentionLabelInfo.toGovernanceLabel() = GovernanceLabel(id, name, color)
    private fun HoldLabelInfo.toGovernanceLabel() = GovernanceLabel(id, name, color)

    companion object {
        private val TAG = FileInfoRepository::class.java.simpleName
        private const val ENTITY_TYPE_FILES = "FILES"
    }
}
