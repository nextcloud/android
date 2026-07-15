/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.android.lib.resources.governance.LabelType
import com.nextcloud.client.account.User
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.owncloud.android.datamodel.OCFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileInfoViewModel @Inject constructor(private val repository: FileInfoRepository) : ViewModel() {

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

    fun init(file: OCFile, user: User) {
        cachedFile = file
        cachedUser = user
        viewModelScope.launch {
            _sensitivityLabels.value = repository.fetchSensitivityLabels(file, user)
        }
        viewModelScope.launch {
            _retentionLabels.value = repository.fetchRetentionLabels(file, user)
        }
        viewModelScope.launch {
            _holdLabels.value = repository.fetchHoldLabels(file, user)
        }
        viewModelScope.launch {
            val entityLabels = repository.fetchEntityLabels(file, user)
            _currentSensitivityLabelId.value = entityLabels.sensitivityId
            _currentRetentionLabelIds.value = entityLabels.retentionIds
            _currentHoldLabelIds.value = entityLabels.holdIds
        }
    }

    fun setSensitivityLabel(labelId: String) {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        viewModelScope.launch {
            if (repository.setLabel(file, user, LabelType.SENSITIVITY, labelId)) {
                _currentSensitivityLabelId.value = labelId
            }
        }
    }

    fun removeSensitivityLabel() {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        viewModelScope.launch {
            val labelId = _currentSensitivityLabelId.value?.takeIf { it.isNotEmpty() } ?: return@launch
            if (repository.removeLabel(file, user, LabelType.SENSITIVITY, labelId)) {
                _currentSensitivityLabelId.value = ""
            }
        }
    }

    fun updateRetentionLabels(newLabelIds: Set<String>) {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        val currentIds = _currentRetentionLabelIds.value ?: emptySet()
        val toAdd = newLabelIds - currentIds
        val toRemove = currentIds - newLabelIds
        viewModelScope.launch {
            toAdd.forEach { repository.setLabel(file, user, LabelType.RETENTION, it) }
            toRemove.forEach { repository.removeLabel(file, user, LabelType.RETENTION, it) }
            _currentRetentionLabelIds.value = newLabelIds
        }
    }

    fun updateHoldLabels(newLabelIds: Set<String>) {
        val file = cachedFile ?: return
        val user = cachedUser ?: return
        val currentIds = _currentHoldLabelIds.value ?: emptySet()
        val toAdd = newLabelIds - currentIds
        val toRemove = currentIds - newLabelIds
        viewModelScope.launch {
            toAdd.forEach { repository.setLabel(file, user, LabelType.HOLD, it) }
            toRemove.forEach { repository.removeLabel(file, user, LabelType.HOLD, it) }
            _currentHoldLabelIds.value = newLabelIds
        }
    }
}
