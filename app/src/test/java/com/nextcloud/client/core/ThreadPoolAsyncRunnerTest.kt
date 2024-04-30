/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import android.os.Handler
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ThreadPoolAsyncRunnerTest {

    private lateinit var handler: Handler
    private lateinit var r: ThreadPoolAsyncRunner

    private companion object {
        const val INIT_COUNT = 1
        const val THREAD_SLEEP = 500L
    }

    @Before
    fun setUp() {
        handler = spy(Handler())
        r = ThreadPoolAsyncRunner(handler, 1)
    }

    fun assertAwait(latch: CountDownLatch, seconds: Long = 3) {
        val called = latch.await(seconds, TimeUnit.SECONDS)
        assertTrue(called)
    }

    @Test
    fun `posted task is run on background thread`() {
        val latch = CountDownLatch(1)
        val callerThread = Thread.currentThread()
        var taskThread: Thread? = null
        r.postQuickTask({
            taskThread = Thread.currentThread()
            latch.countDown()
        })
        assertAwait(latch)
        assertNotEquals(callerThread.id, taskThread?.id)
    }

    @Test
    fun `returns result via handler`() {
        val afterPostLatch = CountDownLatch(1)
        doAnswer {
            (it.arguments[0] as Runnable).run()
            afterPostLatch.countDown()
        }.whenever(handler).post(any())

        val onResult: OnResultCallback<String> = mock()
        r.postQuickTask(
            {
                "result"
            },
            onResult = onResult
        )
        assertAwait(afterPostLatch)
        verify(onResult).invoke(eq("result"))
    }

    @Test
    fun `returns error via handler`() {
        val afterPostLatch = CountDownLatch(INIT_COUNT)
        doAnswer {
            (it.arguments[0] as Runnable).run()
            afterPostLatch.countDown()
        }.whenever(handler).post(any())

        val onResult: OnResultCallback<String> = mock()
        val onError: OnErrorCallback = mock()
        r.postQuickTask(
            {
                throw IllegalArgumentException("whatever")
            },
            onResult = onResult,
            onError = onError
        )
        assertAwait(afterPostLatch)
        verify(onResult, never()).invoke(any())
        verify(onError).invoke(argThat { this is java.lang.IllegalArgumentException })
    }

    @Test
    fun `cancelled task does not return result`() {
        val taskIsCancelled = CountDownLatch(INIT_COUNT)
        val taskIsRunning = CountDownLatch(INIT_COUNT)
        val t = r.postQuickTask(
            {
                taskIsRunning.countDown()
                taskIsCancelled.await()
                "result"
            },
            onResult = {},
            onError = {}
        )
        assertAwait(taskIsRunning)
        t.cancel()
        taskIsCancelled.countDown()
        Thread.sleep(THREAD_SLEEP) // yuck!
        verify(handler, never()).post(any())
    }

    @Test
    fun `cancelled task does not return error`() {
        val taskIsCancelled = CountDownLatch(INIT_COUNT)
        val taskIsRunning = CountDownLatch(INIT_COUNT)
        val t = r.postQuickTask(
            {
                taskIsRunning.countDown()
                taskIsCancelled.await()
                throw IllegalStateException("whatever")
            },
            onResult = {},
            onError = {}
        )
        assertAwait(taskIsRunning)
        t.cancel()
        taskIsCancelled.countDown()
        Thread.sleep(THREAD_SLEEP) // yuck!
        verify(handler, never()).post(any())
    }
}
