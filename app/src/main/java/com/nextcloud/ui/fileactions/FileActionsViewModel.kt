/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.ui.fileactions

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.account.CurrentAccountProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.ui.activity.ComponentsGetter
import javax.inject.Inject

class FileActionsViewModel @Inject constructor(
    private val currentAccountProvider: CurrentAccountProvider,
    private val filterFactory: FileMenuFilter.Factory
) :
    ViewModel() {

    sealed interface UiState {
        object Loading : UiState
        class Loaded(val actions: List<FileAction>) : UiState
    }

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.Loading)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _clickActionId: MutableLiveData<Int?> = MutableLiveData(null)
    val clickActionId: LiveData<Int?>
        @IdRes
        get() = _clickActionId

    fun load(
        files: Collection<OCFile>,
        componentsGetter: ComponentsGetter,
        numberOfAllFiles: Int?,
        isOverflow: Boolean?
    ) {
        val toHide = filterFactory.newInstance(
            numberOfAllFiles ?: 1,
            files.toList(),
            componentsGetter,
            isOverflow ?: false,
            currentAccountProvider.user
        )
            .getToHide(false)
        val availableActions = FileAction.SORTED_VALUES
            .filter { it.id !in toHide }
        _uiState.value = UiState.Loaded(availableActions)
    }

    fun onClick(action: FileAction) {
        _clickActionId.value = action.id
    }
}
