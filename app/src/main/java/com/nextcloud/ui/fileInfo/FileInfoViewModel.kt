/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.android.lib.resources.governance.GetAvailableRetentionLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.GetAvailableSensitivityLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.RetentionLabelInfo
import com.nextcloud.android.lib.resources.governance.SensitivityLabelInfo
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

class FileInfoViewModel @Inject constructor(
    private val clientFactory: ClientFactory
) : ViewModel() {

    private val _sensitivityLabels = MutableStateFlow<List<GovernanceLabel>?>(null)
    val sensitivityLabels: StateFlow<List<GovernanceLabel>?> = _sensitivityLabels

    private val _retentionLabels = MutableStateFlow<List<GovernanceLabel>?>(null)
    val retentionLabels: StateFlow<List<GovernanceLabel>?> = _retentionLabels

    fun load(file: OCFile, user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            _sensitivityLabels.value = fetchSensitivityLabels(file, user)
        }
        viewModelScope.launch(Dispatchers.IO) {
            _retentionLabels.value = fetchRetentionLabels(file, user)
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

    private fun SensitivityLabelInfo.toGovernanceLabel() = GovernanceLabel(name, color)

    private fun RetentionLabelInfo.toGovernanceLabel() = GovernanceLabel(name, color)

    companion object {
        private val TAG = FileInfoViewModel::class.java.simpleName
        private const val ENTITY_TYPE_FILES = "FILES"
    }
}
