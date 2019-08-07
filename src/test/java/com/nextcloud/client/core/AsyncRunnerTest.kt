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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AsyncRunnerTest {

    private lateinit var handler: Handler
    private lateinit var r: AsyncRunnerImpl

    @Before
    fun setUp() {
        handler = spy(Handler())
        r = AsyncRunnerImpl(handler, 1)
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
        r.post({
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
        r.post({
            "result"
        }, onResult = onResult)
        assertAwait(afterPostLatch)
        verify(onResult).invoke(eq("result"))
    }

    @Test
    fun `returns error via handler`() {
        val afterPostLatch = CountDownLatch(1)
        doAnswer {
            (it.arguments[0] as Runnable).run()
            afterPostLatch.countDown()
        }.whenever(handler).post(any())

        val onResult: OnResultCallback<String> = mock()
        val onError: OnErrorCallback = mock()
        r.post({
            throw IllegalArgumentException("whatever")
        }, onResult = onResult, onError = onError)
        assertAwait(afterPostLatch)
        verify(onResult, never()).invoke(any())
        verify(onError).invoke(argThat { this is java.lang.IllegalArgumentException })
    }

    @Test
    fun `cancelled task does not return result`() {
        val taskIsCancelled = CountDownLatch(1)
        val taskIsRunning = CountDownLatch(1)
        val t = r.post({
            taskIsRunning.countDown()
            taskIsCancelled.await()
            "result"
        }, onResult = {}, onError = {})
        assertAwait(taskIsRunning)
        t.cancel()
        taskIsCancelled.countDown()
        Thread.sleep(500) // yuck!
        verify(handler, never()).post(any())
    }

    @Test
    fun `cancelled task does not return error`() {
        val taskIsCancelled = CountDownLatch(1)
        val taskIsRunning = CountDownLatch(1)
        val t = r.post({
            taskIsRunning.countDown()
            taskIsCancelled.await()
            throw RuntimeException("whatever")
        }, onResult = {}, onError = {})
        assertAwait(taskIsRunning)
        t.cancel()
        taskIsCancelled.countDown()
        Thread.sleep(500) // yuck!
        verify(handler, never()).post(any())
    }
}
