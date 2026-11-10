/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.logger

import android.os.Handler
import com.nextcloud.client.core.ClockImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DebugLoggingTest {
    @get:Rule
    val directory = TemporaryFolder()

    @Test
    fun normalLogsRemainAvailableWhenDebugLoggingIsDisabled() {
        val logger = createLogger { false }
        logger.d("test", "debug")
        logger.d("test", "debug exception", IllegalStateException())
        logger.v("test", "verbose")
        logger.i("test", "info")
        logger.w("test", "warning")
        logger.e("test", "error")
        logger.e("test", "error exception", IllegalStateException())

        assertEquals(listOf(Level.INFO, Level.WARNING, Level.ERROR, Level.ERROR), load(logger).map { it.level })
    }

    @Test
    fun switchingDebugLoggingTakesEffectWithoutRestartingLogger() {
        var enabled = false
        val logger = createLogger { enabled }
        logger.d("test", "before enabling")
        enabled = true
        logger.d("test", "debug")
        logger.d("test", "debug exception", IllegalStateException())
        logger.v("test", "verbose")
        enabled = false
        logger.d("test", "after disabling")
        logger.v("test", "after disabling verbose")

        assertEquals(listOf("debug", "debug exception", "verbose"), load(logger).map { it.message })
    }

    private fun createLogger(isDebugEnabled: () -> Boolean): LoggerImpl {
        val handler: Handler = mock()
        whenever(handler.post(any())).thenAnswer {
            it.getArgument<Runnable>(0).run()
            true
        }
        val logHandler = FileLogHandler(directory.newFolder(), "log.txt", LOG_SIZE)
        return LoggerImpl(ClockImpl(), logHandler, handler, QUEUE_CAPACITY, isDebugEnabled).apply { start() }
    }

    private fun load(logger: LoggerImpl): List<LogEntry> {
        val loaded = CountDownLatch(1)
        var entries = emptyList<LogEntry>()
        logger.load { logs, _ ->
            entries = logs
            loaded.countDown()
        }
        assertTrue("Log snapshot was not delivered", loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        return entries
    }

    companion object {
        private const val LOG_SIZE = 4096L
        private const val QUEUE_CAPACITY = 100
        private const val TIMEOUT_SECONDS = 3L
    }
}
