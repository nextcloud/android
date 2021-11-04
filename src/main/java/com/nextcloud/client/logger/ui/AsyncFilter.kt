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

import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Cancellable

/**
 * This utility class allows implementation of as-you-type filtering of large collections.
 *
 * It asynchronously filters collection in background and provide result via callback on the main thread.
 * If new filter request is posted before current filtering task completes, request
 * is stored as pending and is handled after currently running task completes.
 *
 * If a request is already running, another request is already pending and new request is posted
 * (ex. if somebody types faster than live search can finish), the pending request is overwritten
 * by a new one.
 */
class AsyncFilter(private val asyncRunner: AsyncRunner, private val time: () -> Long = System::currentTimeMillis) {

    private var filterTask: Cancellable? = null
    private var pendingRequest: (() -> Unit)? = null
    private val isRunning get() = filterTask != null
    private var startTime = 0L

    /**
     * Schedule filtering request.
     *
     * @param collection items to appy fitler to; items should not be modified when request is being processed
     * @param predicate filter predicate
     * @param onResult result callback called on the main thread
     */
    fun <T> filter(
        collection: Iterable<T>,
        predicate: (T) -> Boolean,
        onResult: (filtered: List<T>, durationMs: Long) -> Unit
    ) {
        pendingRequest = {
            filterAsync(collection, predicate, onResult)
        }
        if (!isRunning) {
            pendingRequest?.invoke()
        }
    }

    private fun <T> filterAsync(collection: Iterable<T>, predicate: (T) -> Boolean, onResult: (List<T>, Long) -> Unit) {
        startTime = time.invoke()
        filterTask = asyncRunner.postQuickTask(
            task = { collection.filter { predicate.invoke(it) } },
            onResult = { filtered: List<T> ->
                onFilterCompleted(filtered, onResult)
            }
        )
        pendingRequest = null
    }

    private fun <T> onFilterCompleted(filtered: List<T>, callback: (List<T>, Long) -> Unit) {
        val dt = time.invoke() - startTime
        callback.invoke(filtered, dt)
        filterTask = null
        startTime = 0L
        pendingRequest?.invoke()
    }
}
