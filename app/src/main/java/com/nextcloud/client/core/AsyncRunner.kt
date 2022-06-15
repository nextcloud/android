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
