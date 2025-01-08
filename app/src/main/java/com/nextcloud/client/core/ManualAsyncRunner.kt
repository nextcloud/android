/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import java.util.ArrayDeque

/**
 * This async runner is suitable for tests, where manual simulation of
 * asynchronous operations is desirable.
 */
class ManualAsyncRunner : AsyncRunner {

    private val queue: ArrayDeque<Runnable> = ArrayDeque()

    override fun <T> postQuickTask(
        task: () -> T,
        onResult: OnResultCallback<T>?,
        onError: OnErrorCallback?
    ): Cancellable = postTask(
        task = { _: OnProgressCallback<Any>, _: IsCancelled -> task.invoke() },
        onResult = onResult,
        onError = onError,
        onProgress = null
    )

    override fun <T, P> postTask(
        task: TaskFunction<T, P>,
        onResult: OnResultCallback<T>?,
        onError: OnErrorCallback?,
        onProgress: OnProgressCallback<P>?
    ): Cancellable {
        val remove: Function1<Runnable, Boolean> = queue::remove
        val taskWrapper = Task(
            postResult = {
                it.run()
                true
            },
            removeFromQueue = remove,
            taskBody = task,
            onSuccess = onResult,
            onError = onError,
            onProgress = onProgress
        )
        queue.push(taskWrapper)
        return taskWrapper
    }

    val size: Int get() = queue.size
    val isEmpty: Boolean get() = queue.size == 0

    /**
     * Run all enqueued tasks until queue is empty. This will run also tasks
     * enqueued by task callbacks.
     *
     * @param maximum max number of tasks to run to avoid infinite loopss
     * @return number of executed tasks
     */
    fun runAll(maximum: Int = 100): Int {
        var c = 0
        while (queue.size > 0) {
            val t = queue.remove()
            t.run()
            c++
            if (c > maximum) {
                throw IllegalStateException("Maximum number of tasks run. Are you in infinite loop?")
            }
        }
        return c
    }

    /**
     * Run one pending task
     *
     * @return true if task has been run
     */
    fun runOne(): Boolean {
        val t = queue.pollFirst()
        t?.run()
        return t != null
    }
}
