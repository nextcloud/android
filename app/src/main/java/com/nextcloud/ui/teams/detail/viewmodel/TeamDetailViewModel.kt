/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.android.lib.resources.teams.get.resources.model.TeamResource
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.ui.teams.detail.model.AddTeamMemberStatus
import com.nextcloud.ui.teams.detail.model.TeamDetailListUiState
import com.nextcloud.ui.teams.detail.model.TeamMemberSuggestion
import com.nextcloud.ui.teams.detail.model.TeamResourceProviderId
import com.nextcloud.ui.teams.detail.model.TeamResourceTarget
import com.nextcloud.ui.teams.detail.model.TeamDetailTab
import com.nextcloud.ui.teams.detail.model.addTeamMemberErrorMessageId
import com.nextcloud.ui.teams.detail.model.loadTeamDetailErrorMessageId
import com.nextcloud.ui.teams.detail.repository.TeamDetailRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TeamDetailViewModel @AssistedInject constructor(
    private val repository: TeamDetailRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val teamId: String = checkNotNull(savedStateHandle[KEY_TEAM_ID])

    private var folderPath: String? = null

    val selectedTab: StateFlow<TeamDetailTab> = savedStateHandle.getStateFlow(KEY_SELECTED_TAB, TeamDetailTab.RESOURCES)

    private val _resources = MutableStateFlow<TeamDetailListUiState<TeamResource>>(TeamDetailListUiState.Loading)
    val resources: StateFlow<TeamDetailListUiState<TeamResource>> = _resources.asStateFlow()

    private val _members = MutableStateFlow<TeamDetailListUiState<TeamMember>>(TeamDetailListUiState.Loading)
    val members: StateFlow<TeamDetailListUiState<TeamMember>> = _members.asStateFlow()

    val addMemberQuery: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_ADD_MEMBER_QUERY, null)

    private val _addMemberStatus = MutableStateFlow<AddTeamMemberStatus>(AddTeamMemberStatus.Editing)
    val addMemberStatus: StateFlow<AddTeamMemberStatus> = _addMemberStatus.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResults = addMemberQuery
        .debounce(SEARCH_DEBOUNCE)
        .mapLatest { query ->
            if (query.isNullOrBlank()) emptyList() else repository.searchMemberSuggestions(query.trim())
        }

    val memberSuggestions: StateFlow<List<TeamMemberSuggestion>> =
        combine(searchResults, _members) { suggestions, members ->
            val currentMembers = (members as? TeamDetailListUiState.Content)?.items.orEmpty()
            val memberIds = currentMembers.mapNotNull { it.userId }.toSet()
            suggestions.filterNot { it.member.id in memberIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    init {
        viewModelScope.launch { loadResources() }
        viewModelScope.launch { loadMembers() }
    }

    fun selectTab(tab: TeamDetailTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab
    }

    fun retryResources() {
        _resources.value = TeamDetailListUiState.Loading
        viewModelScope.launch { loadResources() }
    }

    fun retryMembers() {
        _members.value = TeamDetailListUiState.Loading
        viewModelScope.launch { loadMembers() }
    }

    fun targetFor(resource: TeamResource): TeamResourceTarget {
        val webPage = TeamResourceTarget.WebPage(resource.url, resource.label)
        return when (resource.provider.id) {
            TeamResourceProviderId.TEAM_FOLDER -> folderPath?.let { TeamResourceTarget.Folder(it) } ?: webPage
            TeamResourceProviderId.TALK -> TeamResourceTarget.TalkConversation(resource.id, webPage)
            else -> webPage
        }
    }

    fun openAddMemberDialog() {
        _addMemberStatus.value = AddTeamMemberStatus.Editing
        savedStateHandle[KEY_ADD_MEMBER_QUERY] = ""
    }

    fun updateAddMemberQuery(query: String) {
        _addMemberStatus.value = AddTeamMemberStatus.Editing
        savedStateHandle[KEY_ADD_MEMBER_QUERY] = query
    }

    fun dismissAddMemberDialog() {
        _addMemberStatus.value = AddTeamMemberStatus.Editing
        savedStateHandle[KEY_ADD_MEMBER_QUERY] = null
    }

    fun addMember(suggestion: TeamMemberSuggestion) {
        _addMemberStatus.value = AddTeamMemberStatus.Adding
        viewModelScope.launch {
            val result = repository.addMember(teamId, suggestion.member)
            if (!result.isSuccess) {
                val failed = AddTeamMemberStatus.Failed(result.addTeamMemberErrorMessageId())
                _addMemberStatus.compareAndSet(AddTeamMemberStatus.Adding, failed)
                return@launch
            }

            loadMembers()
            if (_addMemberStatus.compareAndSet(AddTeamMemberStatus.Adding, AddTeamMemberStatus.Editing)) {
                dismissAddMemberDialog()
            }
        }
    }

    private suspend fun loadResources() {
        val result = repository.getResources(teamId)
        if (!result.isSuccess) {
            _resources.value = TeamDetailListUiState.Error(result.loadTeamDetailErrorMessageId())
            return
        }

        val resources = result.resultData.orEmpty()
        if (resources.any { it.provider.id == TeamResourceProviderId.TEAM_FOLDER }) {
            folderPath = loadFolderPath()
        }
        _resources.value = TeamDetailListUiState.Content(resources)
    }

    private suspend fun loadFolderPath(): String? {
        val result = repository.getFolder(teamId)
        if (!result.isSuccess) {
            return null
        }
        return result.resultData?.mountPoint?.let { "/$it/" }
    }

    private suspend fun loadMembers() {
        val result = repository.getMembers(teamId)
        _members.value = if (result.isSuccess) {
            TeamDetailListUiState.Content(result.resultData.orEmpty())
        } else {
            TeamDetailListUiState.Error(result.loadTeamDetailErrorMessageId())
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): TeamDetailViewModel
    }

    companion object {
        const val KEY_TEAM_ID = "team_id"
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_ADD_MEMBER_QUERY = "add_member_query"
        private val SEARCH_DEBOUNCE = 300.milliseconds
        private val STOP_TIMEOUT = 5.seconds
    }
}
