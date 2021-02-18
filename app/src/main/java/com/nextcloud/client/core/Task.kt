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

import java.util.concurrent.atomic.AtomicBoolean

/**
 * This is a wrapper for a task function running in background.
 * It executes task function and handles result or error delivery.
 */
@Suppress("LongParameterList")
internal class Task<T, P>(
    private val postResult: (Runnable) -> Boolean,
    private val removeFromQueue: (Runnable) -> Boolean,
    private val taskBody: TaskFunction<T, P>,
    private val onSuccess: OnResultCallback<T>?,
    private val onError: OnErrorCallback?,
    private val onProgress: OnProgressCallback<P>?
) : Runnable, Cancellable {

    val isCancelled: Boolean
        get() = cancelled.get()

    private val cancelled = AtomicBoolean(false)

    private fun postProgress(p: P) {
        postResult(Runnable { onProgress?.invoke(p) })
    }

    @Suppress("TooGenericExceptionCaught") // this is exactly what we want here
    override fun run() {
        try {
            val result = taskBody.invoke({ postProgress(it) }, this::isCancelled)
            if (!cancelled.get()) {
                postResult.invoke(
                    Runnable {
                        onSuccess?.invoke(result)
                    }
                )
            }
        } catch (t: Throwable) {
            if (!cancelled.get()) {
                postResult(Runnable { onError?.invoke(t) })
            }
        }
        removeFromQueue(this)
    }

    override fun cancel() {
        cancelled.set(true)
        removeFromQueue(this)
    }
}
