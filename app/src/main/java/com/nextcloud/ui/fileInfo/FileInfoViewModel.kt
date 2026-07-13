/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.android.lib.resources.governance.EntityLabels
import com.nextcloud.android.lib.resources.governance.GetAvailableHoldLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.GetAvailableRetentionLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.GetAvailableSensitivityLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.GetEntityLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.HoldLabelInfo
import com.nextcloud.android.lib.resources.governance.LabelType
import com.nextcloud.android.lib.resources.governance.RemoveLabelRemoteOperation
import com.nextcloud.android.lib.resources.governance.RetentionLabelInfo
import com.nextcloud.android.lib.resources.governance.SensitivityLabelInfo
import com.nextcloud.android.lib.resources.governance.SetLabelRemoteOperation
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileInfoViewModel @Inject constructor(private val clientFactory: ClientFactory) : ViewModel() {

    private val _sensitivityLabels = MutableStateFlow<List<GovernanceLabel>?>(null)
    val sensitivityLabels: StateFlow<List<GovernanceLabel>?> = _sensitivityLabels

    private val _retentionLabels = MutableStateFlow<List<GovernanceLabel>?>(null)
    val retentionLabels: StateFlow<List<GovernanceLabel>?> = _retentionLabels

    private val _holdLabels = MutableStateFlow<List<GovernanceLabel>?>(null)
    val holdLabels: StateFlow<List<GovernanceLabel>?> = _holdLabels

    private val _currentSensitivityLabelId = MutableStateFlow<String?>(null)
    val currentSensitivityLabelId: StateFlow<String?> = _currentSensitivityLabelId

    private val _currentRetentionLabelIds = MutableStateFlow<Set<String>?>(null)
    val currentRetentionLabelIds: StateFlow<Set<String>?> = _currentRetentionLabelIds

    private val _currentHoldLabelIds = MutableStateFlow<Set<String>?>(null)
    val currentHoldLabelIds: StateFlow<Set<String>?> = _currentHoldLabelIds

    private var cachedFile: OCFile? = null
    private var cachedUser: User? = null

    fun load(file: OCFile, user: User) {
        cachedFile = file
        cachedUser = user
        viewModelScope.launch(Dispatchers.IO) {
            _sensitivityLabels.value = fetchSensitivityLabels(file, user)
        }
        viewModelScope.launch(Dispatchers.IO) {
            _retentionLabels.value = fetchRetentionLabels(file, user)
        }
        viewModelScope.launch(Dispatchers.IO) {
            _holdLabels.value = fetchHoldLabels(file, user)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val entityLabels = fetchEntityLabels(file, user)
            _currentSensitivityLabelId.value = entityLabels?.sensitivity?.id ?: ""
            _currentRetentionLabelIds.value = entityLabels?.retention?.map { it.id }?.toSet() ?: emptySet()
            _currentHoldLabelIds.value = entityLabels?.hold?.map { it.id }?.toSet() ?: emptySet()
        }
    }

    fun setSensitivityLabel(labelId: String) {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = applyLabel(file, user, LabelType.SENSITIVITY, labelId)
            if (result) {
                _currentSensitivityLabelId.value = labelId
            }
        }
    }

    fun removeSensitivityLabel() {
        val labelId = _currentSensitivityLabelId.value?.takeIf { it.isNotEmpty() } ?: return
        cachedFile?.let { file ->
            cachedUser?.let { user ->
                viewModelScope.launch(Dispatchers.IO) {
                    if (removeLabel(file, user, LabelType.SENSITIVITY, labelId)) {
                        _currentSensitivityLabelId.value = ""
                    }
                }
            }
        }
    }

    fun updateRetentionLabels(newLabelIds: Set<String>) {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        val currentIds = _currentRetentionLabelIds.value ?: emptySet()
        val toAdd = newLabelIds - currentIds
        val toRemove = currentIds - newLabelIds
        viewModelScope.launch(Dispatchers.IO) {
            toAdd.forEach { applyLabel(file, user, LabelType.RETENTION, it) }
            toRemove.forEach { removeLabel(file, user, LabelType.RETENTION, it) }
            _currentRetentionLabelIds.value = newLabelIds
        }
    }

    fun updateHoldLabels(newLabelIds: Set<String>) {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        val currentIds = _currentHoldLabelIds.value ?: emptySet()
        val toAdd = newLabelIds - currentIds
        val toRemove = currentIds - newLabelIds
        viewModelScope.launch(Dispatchers.IO) {
            toAdd.forEach { applyLabel(file, user, LabelType.HOLD, it) }
            toRemove.forEach { removeLabel(file, user, LabelType.HOLD, it) }
            _currentHoldLabelIds.value = newLabelIds
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchSensitivityLabels(file: OCFile, user: User): List<GovernanceLabel> = try {
        val client = clientFactory.createNextcloudClient(user)
        val result = GetAvailableSensitivityLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId).execute(client)
        if (result.isSuccess) result.resultData.orEmpty().map { it.toGovernanceLabel() } else emptyList()
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not fetch available sensitivity labels", e)
        emptyList()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchRetentionLabels(file: OCFile, user: User): List<GovernanceLabel> = try {
        val client = clientFactory.createNextcloudClient(user)
        val result = GetAvailableRetentionLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId).execute(client)
        if (result.isSuccess) result.resultData.orEmpty().map { it.toGovernanceLabel() } else emptyList()
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not fetch available retention labels", e)
        emptyList()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchHoldLabels(file: OCFile, user: User): List<GovernanceLabel> = try {
        val client = clientFactory.createNextcloudClient(user)
        val result = GetAvailableHoldLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId).execute(client)
        if (result.isSuccess) result.resultData.orEmpty().map { it.toGovernanceLabel() } else emptyList()
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not fetch available hold labels", e)
        emptyList()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchEntityLabels(file: OCFile, user: User): EntityLabels? = try {
        val client = clientFactory.createNextcloudClient(user)
        val result = GetEntityLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId.toString()).execute(client)
        if (result.isSuccess) result.resultData else null
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not fetch entity labels", e)
        null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun applyLabel(file: OCFile, user: User, labelType: LabelType, labelId: String): Boolean = try {
        val client = clientFactory.createNextcloudClient(user)
        SetLabelRemoteOperation(ENTITY_TYPE_FILES, file.localId, labelType, labelId).execute(client).isSuccess
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not set label", e)
        false
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeLabel(file: OCFile, user: User, labelType: LabelType, labelId: String): Boolean = try {
        val client = clientFactory.createNextcloudClient(user)
        RemoveLabelRemoteOperation(ENTITY_TYPE_FILES, file.localId, labelType, labelId).execute(client).isSuccess
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not remove label", e)
        false
    }

    private fun SensitivityLabelInfo.toGovernanceLabel() = GovernanceLabel(id, name, color)

    private fun RetentionLabelInfo.toGovernanceLabel() = GovernanceLabel(id, name, color)

    private fun HoldLabelInfo.toGovernanceLabel() = GovernanceLabel(id, name, color)

    companion object {
        private val TAG = FileInfoViewModel::class.java.simpleName
        private const val ENTITY_TYPE_FILES = "FILES"
    }
}
