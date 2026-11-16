/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.utils

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Runs an action at most once per interval and ignores calls while a launched action is still running. */
class Throttle(
    private val interval: Duration = DEFAULT_INTERVAL,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private var lastAccepted: TimeMark? = null
    private var inFlight: Job? = null

    @MainThread
    fun run(action: () -> Unit) {
        if (tryAcquire()) action()
    }

    @MainThread
    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        if (!tryAcquire()) return
        inFlight = scope.launch(block = block)
    }

    private fun tryAcquire(): Boolean {
        if (inFlight?.isActive == true) return false
        if (lastAccepted?.let { it.elapsedNow() < interval } == true) return false

        lastAccepted = timeSource.markNow()
        return true
    }

    companion object {
        val DEFAULT_INTERVAL = 150.milliseconds
    }
}
