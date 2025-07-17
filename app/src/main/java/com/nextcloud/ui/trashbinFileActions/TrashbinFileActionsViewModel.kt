/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.trashbinFileActions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.logger.Logger
import com.nextcloud.ui.fileactions.FileActionsViewModel
import com.owncloud.android.R
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class TrashbinFileActionsViewModel @Inject constructor(private val logger: Logger) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Error : UiState
        data class LoadedForSingleFile(val actions: List<TrashbinFileAction>, val titleFile: TrashbinFile?) : UiState

        data class LoadedForMultipleFiles(val actions: List<TrashbinFileAction>, val fileCount: Int) : UiState
    }

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.Loading)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _clickActionId: MutableLiveData<Int?> = MutableLiveData(null)
    val clickActionId: LiveData<Int?>
        @IdRes
        get() = _clickActionId

    fun load(arguments: Bundle) {
        val files: List<TrashbinFile>? = arguments.getParcelableArrayList(ARG_FILES)
        val numberOfAllFiles: Int = arguments.getInt(FileActionsViewModel.ARG_ALL_FILES_COUNT, 1)

        if (files.isNullOrEmpty()) {
            logger.d(TAG, "No valid files argument for loading actions")
            _uiState.postValue(UiState.Error)
        } else {
            load(files.toList(), numberOfAllFiles)
        }
    }

    private fun load(files: Collection<TrashbinFile>, numberOfAllFiles: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            val toHide = getHiddenActions(numberOfAllFiles, files)
            val availableActions = getActionsToShow(toHide)
            updateStateLoaded(files, availableActions)
        }
    }

    private fun getHiddenActions(numberOfAllFiles: Int?, files: Collection<TrashbinFile>): List<Int> {
        numberOfAllFiles?.let {
            if (files.size >= it) {
                return listOf(R.id.action_select_all_action_menu)
            }
        }

        return listOf()
    }

    private fun getActionsToShow(toHide: List<Int>) = TrashbinFileAction.SORTED_VALUES.filter { it.id !in toHide }

    private fun updateStateLoaded(files: Collection<TrashbinFile>, availableActions: List<TrashbinFileAction>) {
        val state: UiState = when (files.size) {
            1 -> {
                val file = files.first()
                UiState.LoadedForSingleFile(availableActions, file)
            }

            else -> UiState.LoadedForMultipleFiles(availableActions, files.size)
        }
        _uiState.postValue(state)
    }

    fun onClick(action: TrashbinFileAction) {
        _clickActionId.value = action.id
    }

    companion object {
        const val ARG_ALL_FILES_COUNT = "ALL_FILES_COUNT"
        const val ARG_FILES = "FILES"

        private val TAG = TrashbinFileActionsViewModel::class.simpleName!!
    }
}
