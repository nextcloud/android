/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger.ui

import android.annotation.SuppressLint
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

@SuppressLint("StaticFieldLeak")
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
