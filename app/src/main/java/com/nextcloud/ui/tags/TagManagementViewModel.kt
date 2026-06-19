/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.ui.tags.model.TagUiState
import com.nextcloud.ui.tags.model.toLoaded
import com.nextcloud.ui.tags.repository.TagManagementRepository
import com.owncloud.android.lib.resources.tags.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TagManagementViewModel(private val repository: TagManagementRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TagUiState>(TagUiState.Loading)
    val uiState: StateFlow<TagUiState> = _uiState

    private var fileId: Long = -1

    fun fetch(fileId: Long, currentTags: List<Tag>) {
        this.fileId = fileId
        viewModelScope.launch {
            val tags = repository.fetch(fileId, currentTags)

            // TODO: handle error ui state
            _uiState.update {
                tags.toLoaded(currentTags)
            }
        }
    }

    fun assignTag(tag: Tag) {
        viewModelScope.launch {
            val result = repository.assignTag(fileId, tag)
            if (result) {
                _uiState.update { state ->
                    if (state is TagUiState.Loaded) {
                        state.copy(assignedTagIds = state.assignedTagIds + tag.id)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun unassignTag(tag: Tag) {
        viewModelScope.launch {
            val result = repository.unassignTag(fileId, tag)
            if (result) {
                _uiState.update { state ->
                    if (state is TagUiState.Loaded) {
                        state.copy(assignedTagIds = state.assignedTagIds - tag.id)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun createAndAssignTag(name: String) {
        viewModelScope.launch {
            val (allTags, newTagId) = repository.createAndAssignTag(fileId, name) ?: return@launch
            _uiState.update { state ->
                if (state is TagUiState.Loaded) {
                    state.copy(
                        allTags = allTags,
                        assignedTagIds = state.assignedTagIds + newTagId
                    )
                } else {
                    TagUiState.Loaded(
                        allTags = allTags,
                        assignedTagIds = setOf(newTagId)
                    )
                }
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
