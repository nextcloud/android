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

typealias OnResultCallback<T> = (T) -> Unit
typealias OnErrorCallback = (Throwable) -> Unit

/**
 * This interface allows to post background tasks that report results via callbacks invoked on main thread.
 *
 * It is provided as an alternative for heavy, platform specific and virtually untestable [android.os.AsyncTask]
 */
interface AsyncRunner {

    /**
     * Post a background task and return immediately returning task cancellation interface.
     *
     * @param task Task function returning result T; error shall be signalled by throwing an exception.
     * @param onResult Callback called when task function returns a result
     * @param onError Callback called when task function throws an exception
     * @return Cancellable interface, allowing to cancel running task.
     */
    fun <T> post(task: () -> T, onResult: OnResultCallback<T>? = null, onError: OnErrorCallback? = null): Cancellable
}
