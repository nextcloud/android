/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.fileactions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.logger.Logger
import com.nextcloud.utils.TimeConstants
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.ui.activity.ComponentsGetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileActionsViewModel @Inject constructor(
    private val currentAccountProvider: CurrentAccountProvider,
    private val filterFactory: FileMenuFilter.Factory,
    private val logger: Logger
) :
    ViewModel() {

    data class LockInfo(val lockType: FileLockType, val lockedBy: String, val lockedUntil: Long?)

    sealed interface UiState {
        object Loading : UiState
        object Error : UiState
        data class LoadedForSingleFile(
            val actions: List<FileAction>,
            val titleFile: OCFile?,
            val lockInfo: LockInfo? = null
        ) : UiState

        data class LoadedForMultipleFiles(val actions: List<FileAction>, val fileCount: Int) : UiState
    }

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.Loading)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _clickActionId: MutableLiveData<Int?> = MutableLiveData(null)
    val clickActionId: LiveData<Int?>
        @IdRes
        get() = _clickActionId

    fun load(arguments: Bundle, componentsGetter: ComponentsGetter) {
        val files: List<OCFile>? = arguments.getParcelableArrayList(ARG_FILES)
        val numberOfAllFiles: Int = arguments.getInt(ARG_ALL_FILES_COUNT, 1)
        val isOverflow = arguments.getBoolean(ARG_IS_OVERFLOW, false)
        val additionalFilter: IntArray? = arguments.getIntArray(ARG_ADDITIONAL_FILTER)
        val inSingleFileFragment = arguments.getBoolean(ARG_IN_SINGLE_FILE_FRAGMENT)

        if (files.isNullOrEmpty()) {
            logger.d(TAG, "No valid files argument for loading actions")
            _uiState.postValue(UiState.Error)
        } else {
            load(componentsGetter, files.toList(), numberOfAllFiles, isOverflow, additionalFilter, inSingleFileFragment)
        }
    }

    private fun load(
        componentsGetter: ComponentsGetter,
        files: Collection<OCFile>,
        numberOfAllFiles: Int?,
        isOverflow: Boolean?,
        additionalFilter: IntArray?,
        inSingleFileFragment: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val toHide = getHiddenActions(componentsGetter, numberOfAllFiles, files, isOverflow, inSingleFileFragment)
            val availableActions = getActionsToShow(additionalFilter, toHide)
            updateStateLoaded(files, availableActions)
        }
    }

    private fun getHiddenActions(
        componentsGetter: ComponentsGetter,
        numberOfAllFiles: Int?,
        files: Collection<OCFile>,
        isOverflow: Boolean?,
        inSingleFileFragment: Boolean
    ): List<Int> {
        return filterFactory.newInstance(
            numberOfAllFiles ?: 1,
            files.toList(),
            componentsGetter,
            isOverflow ?: false,
            currentAccountProvider.user
        )
            .getToHide(inSingleFileFragment)
    }

    private fun getActionsToShow(additionalFilter: IntArray?, toHide: List<Int>) = FileAction.SORTED_VALUES
        .filter { additionalFilter == null || it.id !in additionalFilter }
        .filter { it.id !in toHide }

    private fun updateStateLoaded(files: Collection<OCFile>, availableActions: List<FileAction>) {
        val state: UiState = when (files.size) {
            1 -> {
                val file = files.first()
                UiState.LoadedForSingleFile(availableActions, file, getLockInfo(file))
            }
            else -> UiState.LoadedForMultipleFiles(availableActions, files.size)
        }
        _uiState.postValue(state)
    }

    private fun getLockInfo(file: OCFile): LockInfo? {
        val lockType = file.lockType
        val username = file.lockOwnerDisplayName ?: file.lockOwnerId
        return if (file.isLocked && lockType != null && username != null) {
            LockInfo(lockType, username, getLockedUntil(file))
        } else {
            null
        }
    }

    private fun getLockedUntil(file: OCFile): Long? {
        return if (file.lockTimestamp == 0L || file.lockTimeout == 0L) {
            null
        } else {
            (file.lockTimestamp + file.lockTimeout) * TimeConstants.MILLIS_PER_SECOND
        }
    }

    fun onClick(action: FileAction) {
        _clickActionId.value = action.id
    }

    companion object {
        const val ARG_ALL_FILES_COUNT = "ALL_FILES_COUNT"
        const val ARG_FILES = "FILES"
        const val ARG_IS_OVERFLOW = "OVERFLOW"
        const val ARG_ADDITIONAL_FILTER = "ADDITIONAL_FILTER"
        const val ARG_IN_SINGLE_FILE_FRAGMENT = "IN_SINGLE_FILE_FRAGMENT"

        private val TAG = FileActionsViewModel::class.simpleName!!
    }
}
