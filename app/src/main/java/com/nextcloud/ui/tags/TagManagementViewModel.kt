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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.tags.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TagManagementViewModel(private val repository: TagManagementRepository) : ViewModel() {

    companion object {
        private const val TAG = "TagManagementViewModel"
    }

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

    fun assignTag(tag: Tag) = setTagAssigned(tag, assign = true)

    fun unassignTag(tag: Tag) = setTagAssigned(tag, assign = false)

    // TODO: handle error ui state
    private fun setTagAssigned(tag: Tag, assign: Boolean) {
        val loaded = _uiState.value as? TagUiState.Loaded ?: return
        if ((tag.id in loaded.assignedTagIds) == assign) return

        fun apply(assigned: Boolean) = _uiState.update { state ->
            if (state is TagUiState.Loaded) {
                state.copy(
                    assignedTagIds = if (assigned) state.assignedTagIds + tag.id else state.assignedTagIds - tag.id
                )
            } else {
                state
            }
        }

        apply(assign)

        viewModelScope.launch {
            val success = if (assign) repository.assignTag(fileId, tag) else repository.unassignTag(fileId, tag)
            if (!success) {
                Log_OC.e(TAG, "cannot ${if (assign) "assign" else "unassign"} tag")
                apply(!assign)
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
