/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.ui.teams.model.TeamsUiState
import com.nextcloud.ui.teams.repository.TeamsRepository
import com.owncloud.android.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeamsViewModel @AssistedInject constructor(
    private val repository: TeamsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()

    private val _messageId = MutableStateFlow<Int?>(null)
    val messageId: StateFlow<Int?> = _messageId.asStateFlow()

    val createTeamName: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_CREATE_TEAM_NAME, null)

    init {
        viewModelScope.launch { loadTeams() }
    }

    fun retry() {
        _uiState.value = TeamsUiState.Loading
        viewModelScope.launch { loadTeams() }
    }

    fun openCreateTeamDialog() {
        savedStateHandle[KEY_CREATE_TEAM_NAME] = ""
    }

    fun updateCreateTeamName(name: String) {
        savedStateHandle[KEY_CREATE_TEAM_NAME] = name
    }

    fun dismissCreateTeamDialog() {
        savedStateHandle[KEY_CREATE_TEAM_NAME] = null
    }

    fun createTeam() {
        val name = createTeamName.value?.trim()
        if (name.isNullOrEmpty()) {
            return
        }

        dismissCreateTeamDialog()
        viewModelScope.launch {
            if (repository.createTeam(name) == null) {
                showMessage(R.string.teams_create_failed)
                return@launch
            }
            loadTeams()
        }
    }

    fun onMessageShown() {
        _messageId.value = null
    }

    private suspend fun loadTeams() {
        val teams = repository.getTeams()
        if (teams == null) {
            _uiState.value = TeamsUiState.Error
            return
        }

        val (myTeams, otherTeams) = teams.partition { it.isMember }
        _uiState.value = TeamsUiState.Content(myTeams, otherTeams)
    }

    private fun showMessage(@StringRes id: Int) {
        _messageId.value = id
    }

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): TeamsViewModel
    }

    companion object {
        private const val KEY_CREATE_TEAM_NAME = "create_team_name"
    }
}
