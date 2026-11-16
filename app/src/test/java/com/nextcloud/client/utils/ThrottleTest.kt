/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class ThrottleTest {

    private val timeSource = TestTimeSource()
    private val sut = Throttle(INTERVAL, timeSource)
    private var calls = 0

    @Test
    fun `repeated calls at the same instant run only once`() {
        repeat(10) { sut.run { calls++ } }

        assertEquals(1, calls)
    }

    @Test
    fun `calls spaced by the interval all run`() {
        repeat(4) {
            sut.run { calls++ }
            timeSource += INTERVAL
        }

        assertEquals(4, calls)
    }

    @Test
    fun `calls inside the interval are dropped`() {
        repeat(4) {
            sut.run { calls++ }
            timeSource += HALF_INTERVAL
        }

        assertEquals(2, calls)
    }

    @Test
    fun `launch is ignored while the previous job is still running`() {
        val scope = TestScope(StandardTestDispatcher())
        val gate = CompletableDeferred<Unit>()

        sut.launch(scope) {
            calls++
            gate.await()
        }
        scope.runCurrent()
        timeSource += INTERVAL * 2
        sut.launch(scope) { calls++ }
        scope.runCurrent()

        assertEquals(1, calls)

        gate.complete(Unit)
        scope.advanceUntilIdle()
        sut.launch(scope) { calls++ }
        scope.advanceUntilIdle()

        assertEquals(2, calls)
    }

    private companion object {
        val INTERVAL = 150.milliseconds
        val HALF_INTERVAL = 75.milliseconds
    }
}
