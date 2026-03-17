/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.lib.resources.tags.CreateTagRemoteOperation
import com.owncloud.android.lib.resources.tags.DeleteTagRemoteOperation
import com.owncloud.android.lib.resources.tags.GetTagsRemoteOperation
import com.owncloud.android.lib.resources.tags.PutTagRemoteOperation
import com.owncloud.android.lib.resources.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class TagManagementViewModel @Inject constructor(
    private val clientFactory: ClientFactory,
    private val currentAccountProvider: CurrentAccountProvider
) : ViewModel() {

    sealed interface TagUiState {
        object Loading : TagUiState
        data class Loaded(val allTags: List<Tag>, val assignedTagIds: Set<String>, val query: String = "") : TagUiState

        data class Error(val message: String) : TagUiState
    }

    private val _uiState = MutableStateFlow<TagUiState>(TagUiState.Loading)
    val uiState: StateFlow<TagUiState> = _uiState

    private var fileId: Long = -1

    fun load(fileId: Long, currentTags: List<Tag>) {
        this.fileId = fileId
        val assignedTagNames = currentTags.map { it.name }.toSet()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.create(currentAccountProvider.user)
                val result = GetTagsRemoteOperation().execute(client)

                if (result.isSuccess) {
                    val assignedIds = result.resultData
                        .filter { it.name in assignedTagNames }
                        .map { it.id }
                        .toSet()

                    _uiState.update {
                        TagUiState.Loaded(
                            allTags = result.resultData,
                            assignedTagIds = assignedIds
                        )
                    }
                } else {
                    _uiState.update { TagUiState.Error("Failed to load tags") }
                }
            } catch (e: ClientFactory.CreationException) {
                _uiState.update { TagUiState.Error("Failed to create client") }
            }
        }
    }

    fun assignTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
                val result = PutTagRemoteOperation(tag.id, fileId).execute(client)

                if (result.isSuccess) {
                    _uiState.update { state ->
                        if (state is TagUiState.Loaded) {
                            state.copy(assignedTagIds = state.assignedTagIds + tag.id)
                        } else {
                            state
                        }
                    }
                }
            } catch (e: ClientFactory.CreationException) {
                // ignore
            }
        }
    }

    fun unassignTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
                val result = DeleteTagRemoteOperation(tag.id, fileId).execute(client)

                if (result.isSuccess) {
                    _uiState.update { state ->
                        if (state is TagUiState.Loaded) {
                            state.copy(assignedTagIds = state.assignedTagIds - tag.id)
                        } else {
                            state
                        }
                    }
                }
            } catch (e: ClientFactory.CreationException) {
                // ignore
            }
        }
    }

    fun createAndAssignTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextcloudClient = clientFactory.createNextcloudClient(currentAccountProvider.user)
                val createResult = CreateTagRemoteOperation(name).execute(nextcloudClient)

                if (createResult.isSuccess) {
                    val ownCloudClient = clientFactory.create(currentAccountProvider.user)
                    val tagsResult = GetTagsRemoteOperation().execute(ownCloudClient)

                    if (tagsResult.isSuccess) {
                        val allTags = tagsResult.resultData
                        val newTag = allTags.find { it.name == name }

                        if (newTag != null) {
                            PutTagRemoteOperation(newTag.id, fileId).execute(nextcloudClient)

                            _uiState.update { state ->
                                if (state is TagUiState.Loaded) {
                                    state.copy(
                                        allTags = allTags,
                                        assignedTagIds = state.assignedTagIds + newTag.id
                                    )
                                } else {
                                    TagUiState.Loaded(
                                        allTags = allTags,
                                        assignedTagIds = setOf(newTag.id)
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: ClientFactory.CreationException) {
                // ignore
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            if (state is TagUiState.Loaded) {
                state.copy(query = query)
            } else {
                state
            }
        }
    }

    fun getAssignedTags(): List<Tag> {
        val state = _uiState.value
        if (state is TagUiState.Loaded) {
            return state.allTags.filter { it.id in state.assignedTagIds }
        }
        return emptyList()
    }
}
