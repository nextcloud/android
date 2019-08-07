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

import android.os.Handler
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

internal class AsyncRunnerImpl(private val uiThreadHandler: Handler, corePoolSize: Int) : AsyncRunner {

    private class Task<T>(
        private val handler: Handler,
        private val callable: () -> T,
        private val onSuccess: OnResultCallback<T>?,
        private val onError: OnErrorCallback?
    ) : Runnable, Cancellable {

        private val cancelled = AtomicBoolean(false)

        override fun run() {
            @Suppress("TooGenericExceptionCaught") // this is exactly what we want here
            try {
                val result = callable.invoke()
                if (!cancelled.get()) {
                    handler.post {
                        onSuccess?.invoke(result)
                    }
                }
            } catch (t: Throwable) {
                if (!cancelled.get()) {
                    handler.post { onError?.invoke(t) }
                }
            }
        }

        override fun cancel() {
            cancelled.set(true)
        }
    }

    private val executor = ScheduledThreadPoolExecutor(corePoolSize)

    override fun <T> post(block: () -> T, onResult: OnResultCallback<T>?, onError: OnErrorCallback?): Cancellable {
        val task = Task(uiThreadHandler, block, onResult, onError)
        executor.execute(task)
        return task
    }
}
