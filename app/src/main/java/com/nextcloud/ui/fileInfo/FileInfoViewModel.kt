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
import com.nextcloud.ui.fileInfo.model.GovernanceUiState
import com.owncloud.android.datamodel.OCFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FileInfoViewModel @AssistedInject constructor(
    private val repository: FileInfoRepository,
    @Assisted private val file: OCFile,
    @Assisted private val user: User
) : ViewModel() {

    private val _uiState = MutableStateFlow<GovernanceUiState>(GovernanceUiState.Loading)
    val uiState: StateFlow<GovernanceUiState> = _uiState

    init {
        loadGovernance()
    }

    private fun loadGovernance() {
        viewModelScope.launch {
            val selectableLabels = async { repository.fetchAllSelectableLabels(file, user) }
            val entityLabels = repository.fetchEntityLabels(file, user)
            val labels = selectableLabels.await()
            _uiState.value = GovernanceUiState.Loaded(
                sensitivityLabels = labels.sensitivityLabels,
                retentionLabels = labels.retentionLabels,
                holdLabels = labels.holdLabels,
                currentSensitivityLabelId = entityLabels.sensitivityId,
                currentRetentionLabelIds = entityLabels.retentionIds,
                currentHoldLabelIds = entityLabels.holdIds
            )
        }
    }

    fun setSensitivityLabel(labelId: String) {
        viewModelScope.launch {
            if (repository.setLabel(file, user, LabelType.SENSITIVITY, labelId)) {
                updateLoaded { it.copy(currentSensitivityLabelId = labelId) }
            }
        }
    }

    fun removeSensitivityLabel() {
        viewModelScope.launch {
            val labelId = loaded()?.currentSensitivityLabelId?.takeIf { it.isNotEmpty() } ?: return@launch
            if (repository.removeLabel(file, user, LabelType.SENSITIVITY, labelId)) {
                updateLoaded { it.copy(currentSensitivityLabelId = "") }
            }
        }
    }

    fun updateRetentionLabels(newLabelIds: Set<String>) {
        val currentIds = loaded()?.currentRetentionLabelIds ?: return
        viewModelScope.launch {
            applyDiff(LabelType.RETENTION, currentIds, newLabelIds)
            updateLoaded { it.copy(currentRetentionLabelIds = newLabelIds) }
        }
    }

    fun updateHoldLabels(newLabelIds: Set<String>) {
        val currentIds = loaded()?.currentHoldLabelIds ?: return
        viewModelScope.launch {
            applyDiff(LabelType.HOLD, currentIds, newLabelIds)
            updateLoaded { it.copy(currentHoldLabelIds = newLabelIds) }
        }
    }

    private suspend fun applyDiff(labelType: LabelType, currentIds: Set<String>, newLabelIds: Set<String>) {
        (newLabelIds - currentIds).forEach { repository.setLabel(file, user, labelType, it) }
        (currentIds - newLabelIds).forEach { repository.removeLabel(file, user, labelType, it) }
    }

    private fun loaded(): GovernanceUiState.Loaded? = _uiState.value as? GovernanceUiState.Loaded

    private fun updateLoaded(transform: (GovernanceUiState.Loaded) -> GovernanceUiState.Loaded) {
        _uiState.update { state ->
            if (state is GovernanceUiState.Loaded) transform(state) else state
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(file: OCFile, user: User): FileInfoViewModel
    }
}
