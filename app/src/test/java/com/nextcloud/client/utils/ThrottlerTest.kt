package com.nextcloud.client.utils

import com.nextcloud.client.core.Clock
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ThrottlerTest {
    companion object {
        private const val KEY = "TEST"
    }

    @MockK
    lateinit var runnable: Runnable

    @MockK
    lateinit var clock: Clock

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        every { runnable.run() } just Runs
    }

    private fun runWithThrottler(throttler: Throttler) {
        throttler.run(KEY, runnable)
    }

    @Test
    fun unchangingTime_multipleCalls_calledExactlyOnce() {
        // given
        every { clock.currentTime } returns 300

        val sut = Throttler(clock).apply {
            intervalMillis = 150
        }

        // when
        repeat(10) {
            runWithThrottler(sut)
        }

        // then
        verify(exactly = 1) { runnable.run() }
    }

    @Test
    fun spacedCalls_noThrottle() {
        // given
        val sut = Throttler(clock).apply {
            intervalMillis = 150
        }
        every { clock.currentTime } returnsMany listOf(200, 400, 600, 800)

        // when
        repeat(4) {
            runWithThrottler(sut)
        }

        // then
        verify(exactly = 4) { runnable.run() }
    }

    @Test
    fun mixedIntervals_sometimesThrottled() {
        // given
        val sut = Throttler(clock).apply {
            intervalMillis = 150
        }
        every { clock.currentTime } returnsMany listOf(200, 300, 400, 500)

        // when
        repeat(4) {
            runWithThrottler(sut)
        }

        // then
        verify(exactly = 2) { runnable.run() }
    }
}
