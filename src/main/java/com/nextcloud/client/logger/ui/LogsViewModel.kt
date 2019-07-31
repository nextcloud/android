/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.logger.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Clock
import com.nextcloud.client.logger.LogEntry
import com.nextcloud.client.logger.LogsRepository
import javax.inject.Inject

class LogsViewModel @Inject constructor(
    context: Context,
    clock: Clock,
    asyncRunner: AsyncRunner,
    private val logsRepository: LogsRepository
) : ViewModel() {

    private val sender = LogsEmailSender(context, clock, asyncRunner)
    val entries: LiveData<List<LogEntry>> = MutableLiveData()
    private val listener = object : LogsRepository.Listener {
        override fun onLoaded(entries: List<LogEntry>) {
            this@LogsViewModel.entries as MutableLiveData
            this@LogsViewModel.entries.value = entries
        }
    }

    fun send() {
        entries.value?.let {
            sender.send(it)
        }
    }

    fun load() {
        logsRepository.load(listener)
    }

    fun deleteAll() {
        logsRepository.deleteAll()
        (entries as MutableLiveData).value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        sender.stop()
    }
}
