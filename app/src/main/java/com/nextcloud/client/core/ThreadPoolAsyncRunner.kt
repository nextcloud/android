/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
internal class ThreadPoolAsyncRunner(
    private val uiThreadHandler: Handler,
    corePoolSize: Int,
    val tag: String = "default"
) : AsyncRunner {

    private val executor = ScheduledThreadPoolExecutor(corePoolSize)

    override fun <T> postQuickTask(
        task: () -> T,
        onResult: OnResultCallback<T>?,
        onError: OnErrorCallback?
    ): Cancellable {
        val taskAdapter = { _: OnProgressCallback<Void>, _: IsCancelled -> task.invoke() }
        return postTask(
            taskAdapter,
            onResult,
            onError,
            null
        )
    }

    override fun <T, P> postTask(
        task: TaskFunction<T, P>,
        onResult: OnResultCallback<T>?,
        onError: OnErrorCallback?,
        onProgress: OnProgressCallback<P>?
    ): Cancellable {
        val remove: Function1<Runnable, Boolean> = executor::remove
        val taskWrapper = Task(
            postResult = uiThreadHandler::post,
            removeFromQueue = remove,
            taskBody = task,
            onSuccess = onResult,
            onError = onError,
            onProgress = onProgress
        )
        executor.execute(taskWrapper)
        return taskWrapper
    }
}
