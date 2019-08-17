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

/**
 * This async runner uses [java.util.concurrent.ScheduledThreadPoolExecutor] to run tasks
 * asynchronously.
 *
 * Tasks are run on multi-threaded pool. If serialized execution is desired, set [corePoolSize] to 1.
 */
internal class ThreadPoolAsyncRunner(private val uiThreadHandler: Handler, corePoolSize: Int) : AsyncRunner {

    private val executor = ScheduledThreadPoolExecutor(corePoolSize)

    override fun <T> post(task: () -> T, onResult: OnResultCallback<T>?, onError: OnErrorCallback?): Cancellable {
        val taskWrapper = Task(this::postResult, task, onResult, onError)
        executor.execute(taskWrapper)
        return taskWrapper
    }

    private fun postResult(r: Runnable) {
        uiThreadHandler.post(r)
    }
}
