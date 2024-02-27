/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.assistant

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.account.User
import com.nextcloud.operations.assistant.AssistantRepository
import com.nextcloud.operations.assistant.model.CreatedTask
import com.nextcloud.operations.assistant.model.TaskTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(context: Context, user: User) : ViewModel() {

    private val repository = AssistantRepository(user, context)

    private val _taskTypes = MutableStateFlow<TaskTypes?>(null)
    val taskTypes: StateFlow<TaskTypes?> = _taskTypes

    private val _task = MutableStateFlow<CreatedTask?>(null)
    val task: StateFlow<CreatedTask?> = _task

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _taskTypes.update {
                repository.getTaskTypes()
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(id)
        }
    }

    fun getTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _task.update {
                repository.getTask(id)
            }
        }
    }

    fun createTask(
        input: String,
        type: String,
        appId: String,
        identifier: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createTask(input, type, appId, identifier)
        }
    }
}
