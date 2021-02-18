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
package com.nextcloud.client.logger

import android.os.Handler
import com.nextcloud.client.core.Clock
import com.nextcloud.client.core.ClockImpl
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.capture
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockitoAnnotations
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LoggerTest {

    private companion object {
        const val QUEUE_CAPACITY = 100
        const val FILE_SIZE = 1024L
        const val LATCH_WAIT = 3L
        const val LATCH_INIT = 3
        const val EMPTY = 0
        const val EMPTY_LONG = 0L
        const val TIMEOUT = 3000L
        const val MESSAGE_COUNT = 3
    }

    private lateinit var clock: Clock
    private lateinit var logHandler: FileLogHandler
    private lateinit var osHandler: Handler
    private lateinit var logger: LoggerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val tempDir = Files.createTempDirectory("log-test").toFile()
        clock = ClockImpl()
        logHandler = spy(FileLogHandler(tempDir, "log.txt", FILE_SIZE))
        osHandler = mock()
        logger = LoggerImpl(clock, logHandler, osHandler, QUEUE_CAPACITY)
    }

    @Test
    fun `write is done on background thread`() {
        val callerThreadId = Thread.currentThread().id
        val writerThreadIds = mutableListOf<Long>()
        val latch = CountDownLatch(LATCH_INIT)

        doAnswer {
            writerThreadIds.add(Thread.currentThread().id)
            it.callRealMethod()
            latch.countDown()
        }.whenever(logHandler).open()

        doAnswer {
            writerThreadIds.add(Thread.currentThread().id)
            it.callRealMethod()
            latch.countDown()
        }.whenever(logHandler).write(any())

        doAnswer {
            writerThreadIds.add(Thread.currentThread().id)
            it.callRealMethod()
            latch.countDown()
        }.whenever(logHandler).close()

        // GIVEN
        //      logger event loop is running
        logger.start()

        // WHEN
        //      message is logged
        logger.d("tag", "message")

        // THEN
        //      message is processed on bg thread
        //      all handler invocations happen on bg thread
        //      all handler invocations happen on single thread
        assertTrue(latch.await(LATCH_WAIT, TimeUnit.SECONDS))

        writerThreadIds.forEach { writerThreadId ->
            assertNotEquals("All requests must be made on bg thread", callerThreadId, writerThreadId)
        }

        writerThreadIds.forEach {
            assertEquals("All requests must be made on single thread", writerThreadIds[0], it)
        }
    }

    @Test
    fun `message is written via log handler`() {
        val tag = "test tag"
        val message = "test log message"
        val latch = CountDownLatch(LATCH_INIT)
        doAnswer { it.callRealMethod(); latch.countDown() }.whenever(logHandler).open()
        doAnswer { it.callRealMethod(); latch.countDown() }.whenever(logHandler).write(any())
        doAnswer { it.callRealMethod(); latch.countDown() }.whenever(logHandler).close()

        // GIVEN
        //      logger event loop is running
        logger.start()

        // WHEN
        //      log message is written
        logger.d(tag, message)

        // THEN
        //      log handler opens log file
        //      log handler writes entry
        //      log handler closes log file
        //      no lost messages
        val called = latch.await(LATCH_WAIT, TimeUnit.SECONDS)
        assertTrue("Expected open(), write() and close() calls on bg thread", called)
        val inOrder = inOrder(logHandler)
        inOrder.verify(logHandler).open()
        inOrder.verify(logHandler).write(
            argThat {
                tag in this && message in this
            }
        )
        inOrder.verify(logHandler).close()
        assertFalse(logger.lostEntries)
    }

    @Test
    fun `logs are loaded in background thread and posted to main thread`() {
        val currentThreadId = Thread.currentThread().id
        var loggerThreadId: Long = -1
        val listener: OnLogsLoaded = mock()
        val latch = CountDownLatch(2)

        // log handler will be called on bg thread
        doAnswer {
            loggerThreadId = Thread.currentThread().id
            latch.countDown()
            it.callRealMethod()
        }.whenever(logHandler).loadLogFiles(any())

        // os handler will be called on bg thread
        whenever(osHandler.post(any())).thenAnswer {
            latch.countDown()
            true
        }

        // GIVEN
        //      logger event loop is running
        logger.start()

        // WHEN
        //      messages are logged
        //      log contents are requested
        logger.d("tag", "message 1")
        logger.d("tag", "message 2")
        logger.d("tag", "message 3")
        logger.load(listener)
        val called = latch.await(LATCH_WAIT, TimeUnit.SECONDS)
        assertTrue("Response not posted", called)

        // THEN
        //      log contents are loaded on background thread
        //      logs are posted to main thread handler
        //      contents contain logged messages
        //      messages are in order of writes
        assertNotEquals(currentThreadId, loggerThreadId)

        val postedCaptor = ArgumentCaptor.forClass(Runnable::class.java)
        verify(osHandler).post(capture(postedCaptor))
        postedCaptor.value.run()

        val logsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<LogEntry>>
        val sizeCaptor = ArgumentCaptor.forClass(Long::class.java)
        verify(listener).invoke(capture(logsCaptor), capture(sizeCaptor))
        assertEquals(MESSAGE_COUNT, logsCaptor.value.size)
        assertTrue("message 1" in logsCaptor.value[0].message)
        assertTrue("message 2" in logsCaptor.value[1].message)
        assertTrue("message 3" in logsCaptor.value[2].message)
    }

    @Test
    fun `log level can be decoded from tags`() {
        Level.values().forEach {
            val decodedLevel = Level.fromTag(it.tag)
            assertEquals(it, decodedLevel)
        }
    }

    @Test
    fun `queue limit is enforced`() {
        // GIVEN
        //      logger event loop is no running

        // WHEN
        //      queue is filled up to it's capacity
        for (i in 0 until QUEUE_CAPACITY + 1) {
            logger.d("tag", "Message $i")
        }

        // THEN
        //      overflow flag is raised
        assertTrue(logger.lostEntries)
    }

    @Test
    fun `queue overflow warning is logged`() {

        // GIVEN
        //      logger loop is overflown
        for (i in 0..QUEUE_CAPACITY + 1) {
            logger.d("tag", "Message $i")
        }

        // WHEN
        //      logger event loop processes events
        //
        logger.start()

        // THEN
        //      overflow occurrence is logged
        val posted = CountDownLatch(1)
        whenever(osHandler.post(any())).thenAnswer {
            (it.arguments[0] as Runnable).run()
            posted.countDown()
            true
        }

        val listener: OnLogsLoaded = mock()
        logger.load(listener)
        assertTrue("Logs not loaded", posted.await(1, TimeUnit.SECONDS))

        verify(listener).invoke(
            argThat {
                "Logger queue overflow" in last().message
            },
            any()
        )
    }

    @Test
    fun `all log files are deleted`() {
        val latch = CountDownLatch(1)
        doAnswer {
            it.callRealMethod()
            latch.countDown()
        }.whenever(logHandler).deleteAll()

        // GIVEN
        //      logger is started
        logger.start()

        // WHEN
        //      logger has some writes
        //      logs are deleted
        logger.d("tag", "message")
        logger.d("tag", "message")
        logger.d("tag", "message")
        logger.deleteAll()

        // THEN
        //      handler writes files
        //      handler deletes all files
        assertTrue(latch.await(LATCH_WAIT, TimeUnit.SECONDS))
        verify(logHandler, times(MESSAGE_COUNT)).write(any())
        verify(logHandler).deleteAll()
        val loaded = logHandler.loadLogFiles(logHandler.maxLogFilesCount)
        assertEquals(EMPTY, loaded.lines.size)
        assertEquals(EMPTY_LONG, loaded.logSize)
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `thread interruption is handled while posting log message`() {
        Thread {
            val callerThread = Thread.currentThread()
            // GIVEN
            //      logger is running
            //      caller thread is interrupted
            logger.start()
            callerThread.interrupt()

            // WHEN
            //      message is logged on interrupted thread
            var loggerException: Throwable? = null
            try {
                logger.d("test", "test")
            } catch (ex: Throwable) {
                loggerException = ex
            }

            // THEN
            //      message post is gracefully skipped
            //      thread interruption flag is not cleared
            assertNull(loggerException)
            assertTrue("Expected current thread to stay interrupted", callerThread.isInterrupted)
        }.apply {
            start()
            join(TIMEOUT)
        }
    }
}
