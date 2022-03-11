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
    ): Cancellable {
        return postTask(
            task = { _: OnProgressCallback<Any>, _: IsCancelled -> task.invoke() },
            onResult = onResult,
            onError = onError,
            onProgress = null
        )
    }

    override fun <T, P> postTask(
        task: TaskFunction<T, P>,
        onResult: OnResultCallback<T>?,
        onError: OnErrorCallback?,
        onProgress: OnProgressCallback<P>?
    ): Cancellable {
        val remove: Function1<Runnable, Boolean> = queue::remove
        val taskWrapper = Task(
            postResult = { it.run(); true },
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
