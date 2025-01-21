/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
) : Runnable,
    Cancellable {

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
