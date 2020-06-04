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
import com.owncloud.android.R
import javax.inject.Inject

class LogsViewModel @Inject constructor(
    private val context: Context,
    clock: Clock,
    asyncRunner: AsyncRunner,
    private val logsRepository: LogsRepository
) : ViewModel() {

    private companion object {
        const val KILOBYTE = 1024L
    }

    private val asyncFilter = AsyncFilter(asyncRunner)
    private val sender = LogsEmailSender(context, clock, asyncRunner)
    private var allEntries = emptyList<LogEntry>()
    private var logsSize = -1L
    private var filterDurationMs = 0L
    private var isFiltered = false

    val isLoading: LiveData<Boolean> = MutableLiveData<Boolean>().apply { value = false }
    val size: LiveData<Long> = MutableLiveData<Long>().apply { value = 0 }
    val entries: LiveData<List<LogEntry>> = MutableLiveData<List<LogEntry>>().apply { value = emptyList() }
    val status: LiveData<String> = MutableLiveData<String>().apply { value = "" }

    fun send() {
        entries.value?.let {
            sender.send(it)
        }
    }

    fun load() {
        if (isLoading.value != true) {
            logsRepository.load(this::onLoaded)
            (isLoading as MutableLiveData).value = true
        }
    }

    private fun onLoaded(entries: List<LogEntry>, logsSize: Long) {
        this.entries as MutableLiveData
        this.isLoading as MutableLiveData
        this.status as MutableLiveData

        this.entries.value = entries
        this.allEntries = entries
        this.logsSize = logsSize
        isLoading.value = false
        this.status.value = formatStatus()
    }

    fun deleteAll() {
        logsRepository.deleteAll()
        (entries as MutableLiveData).value = emptyList()
    }

    fun filter(pattern: String) {
        if (isLoading.value == false) {
            isFiltered = pattern.isNotEmpty()
            val predicate = when (isFiltered) {
                true -> { it: LogEntry -> it.tag.contains(pattern, true) || it.message.contains(pattern, true) }
                false -> { _ -> true }
            }
            asyncFilter.filter(
                collection = allEntries,
                predicate = predicate,
                onResult = this::onFiltered
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sender.stop()
    }

    private fun onFiltered(filtered: List<LogEntry>, filterDurationMs: Long) {
        (entries as MutableLiveData).value = filtered
        this.filterDurationMs = filterDurationMs
        (status as MutableLiveData).value = formatStatus()
    }

    private fun formatStatus(): String {
        val displayedEntries = entries.value?.size ?: allEntries.size
        val sizeKb = logsSize / KILOBYTE
        return when {
            isLoading.value == true -> context.getString(R.string.logs_status_loading)
            isFiltered -> context.getString(
                R.string.logs_status_filtered,
                sizeKb,
                displayedEntries,
                allEntries.size,
                filterDurationMs
            )
            !isFiltered -> context.getString(R.string.logs_status_not_filtered, sizeKb)
            else -> ""
        }
    }
}
