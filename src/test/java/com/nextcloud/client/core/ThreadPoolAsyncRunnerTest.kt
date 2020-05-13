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

import android.os.Handler
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
