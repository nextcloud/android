/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
