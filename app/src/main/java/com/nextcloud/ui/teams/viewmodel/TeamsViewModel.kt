/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.di.SavedStateViewModelCreator
import com.nextcloud.ui.teams.model.CreateTeamStatus
import com.nextcloud.ui.teams.model.TeamsUiState
import com.nextcloud.ui.teams.model.createTeamErrorMessageId
import com.nextcloud.ui.teams.model.loadTeamsErrorMessageId
import com.nextcloud.ui.teams.repository.TeamsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TeamsViewModel @AssistedInject constructor(
    private val repository: TeamsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val teamsState = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

    val uiState: StateFlow<TeamsUiState> = combine(teamsState, searchQuery) { state, query ->
        if (state is TeamsUiState.Content) state.matching(query) else state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), TeamsUiState.Loading)

    val createTeamName: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_CREATE_TEAM_NAME, null)

    private val _createTeamStatus = MutableStateFlow<CreateTeamStatus>(CreateTeamStatus.Editing)
    val createTeamStatus: StateFlow<CreateTeamStatus> = _createTeamStatus.asStateFlow()

    init {
        viewModelScope.launch { loadTeams() }
    }

    fun retry() {
        teamsState.value = TeamsUiState.Loading
        viewModelScope.launch { loadTeams() }
    }

    fun updateSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun openCreateTeamDialog() {
        _createTeamStatus.value = CreateTeamStatus.Editing
        savedStateHandle[KEY_CREATE_TEAM_NAME] = ""
    }

    fun updateCreateTeamName(name: String) {
        _createTeamStatus.value = CreateTeamStatus.Editing
        savedStateHandle[KEY_CREATE_TEAM_NAME] = name
    }

    fun dismissCreateTeamDialog() {
        _createTeamStatus.value = CreateTeamStatus.Editing
        savedStateHandle[KEY_CREATE_TEAM_NAME] = null
    }

    fun createTeam() {
        val name = createTeamName.value?.trim()
        if (name == null || !isValidTeamName(name)) {
            return
        }

        _createTeamStatus.value = CreateTeamStatus.Creating
        viewModelScope.launch {
            val result = repository.createTeam(name)
            if (!result.isSuccess) {
                val failed = CreateTeamStatus.Failed(result.createTeamErrorMessageId())
                _createTeamStatus.compareAndSet(CreateTeamStatus.Creating, failed)
                return@launch
            }

            loadTeams()
            if (_createTeamStatus.compareAndSet(CreateTeamStatus.Creating, CreateTeamStatus.Editing)) {
                dismissCreateTeamDialog()
            }
        }
    }

    private suspend fun loadTeams() {
        val result = repository.getTeams()
        if (!result.isSuccess) {
            teamsState.value = TeamsUiState.Error(result.loadTeamsErrorMessageId())
            return
        }

        val (myTeams, otherTeams) = result.resultData.orEmpty().partition { it.isMember }
        teamsState.value = TeamsUiState.Content(myTeams, otherTeams)
    }

    @AssistedFactory
    interface Factory : SavedStateViewModelCreator {
        override fun create(savedStateHandle: SavedStateHandle): TeamsViewModel
    }

    companion object {
        private const val KEY_CREATE_TEAM_NAME = "create_team_name"
        private const val KEY_SEARCH_QUERY = "search_query"
        private val STOP_TIMEOUT = 5.seconds
        const val MIN_TEAM_NAME_BYTES = 3
        const val MAX_TEAM_NAME_BYTES = 127

        fun isValidTeamName(name: String): Boolean =
            name.trim().toByteArray().size in MIN_TEAM_NAME_BYTES..MAX_TEAM_NAME_BYTES
    }
}
