/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

typealias OnResultCallback<T> = (result: T) -> Unit
typealias OnErrorCallback = (error: Throwable) -> Unit
typealias OnProgressCallback<P> = (progress: P) -> Unit
typealias IsCancelled = () -> Boolean
typealias TaskFunction<RESULT, PROGRESS> = (
    onProgress: OnProgressCallback<PROGRESS>,
    isCancelled: IsCancelled
) -> RESULT

/**
 * This interface allows to post background tasks that report results via callbacks invoked on main thread.
 * It is provided as an alternative for heavy, platform specific and virtually untestable [android.os.AsyncTask]
 *
 * Please note that as of Android R, [android.os.AsyncTask] is deprecated and [java.util.concurrent] is a recommended
 * alternative.
 */
interface AsyncRunner {

    /**
     * Post a quick background task and return immediately returning task cancellation interface.
     *
     * Quick task is a short piece of code that does not support interruption nor progress monitoring.
     *
     * @param task Task function returning result T; error shall be signalled by throwing an exception.
     * @param onResult Callback called when task function returns a result.
     * @param onError Callback called when task function throws an exception.
     * @return Cancellable interface, allowing cancellation of a running task.
     */
    fun <RESULT> postQuickTask(
        task: () -> RESULT,
        onResult: OnResultCallback<RESULT>? = null,
        onError: OnErrorCallback? = null
    ): Cancellable

    /**
     * Post a background task and return immediately returning task cancellation interface.
     *
     * @param task Task function returning result T; error shall be signalled by throwing an exception.
     * @param onResult Callback called when task function returns a result,
     * @param onError Callback called when task function throws an exception.
     * @param onProgress Callback called when task function reports progress update.
     * @return Cancellable interface, allowing cancellation of a running task.
     */
    fun <RESULT, PROGRESS> postTask(
        task: TaskFunction<RESULT, PROGRESS>,
        onResult: OnResultCallback<RESULT>? = null,
        onError: OnErrorCallback? = null,
        onProgress: OnProgressCallback<PROGRESS>? = null
    ): Cancellable
}
