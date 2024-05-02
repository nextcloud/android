/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ManualAsyncRunnerTest {

    private companion object {
        const val EMPTY = 0
        const val ONE_TASK = 1
        const val TWO_TASKS = 2
        const val THREE_TASKS = 3
        const val TIMEOUT = 10000L
    }

    private lateinit var runner: ManualAsyncRunner

    @Mock
    private lateinit var task: () -> Int

    @Mock
    private lateinit var onResult: OnResultCallback<Int>

    @Mock
    private lateinit var onError: OnErrorCallback

    private var taskCalls: Int = EMPTY

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        runner = ManualAsyncRunner()
        taskCalls = EMPTY
        whenever(task.invoke()).thenAnswer {
            taskCalls++
            taskCalls
        }
    }

    @Test
    fun `tasks are queued`() {
        assertEquals(EMPTY, runner.size)
        runner.postQuickTask(task, onResult, onError)
        runner.postQuickTask(task, onResult, onError)
        runner.postQuickTask(task, onResult, onError)
        assertEquals("Expected 3 tasks to be enqueued", THREE_TASKS, runner.size)
    }

    @Test
    fun `run one enqueued task`() {
        runner.postQuickTask(task, onResult, onError)
        runner.postQuickTask(task, onResult, onError)
        runner.postQuickTask(task, onResult, onError)

        assertEquals("Queue should contain all enqueued tasks", THREE_TASKS, runner.size)
        val run = runner.runOne()
        assertTrue("Executed task should be acknowledged", run)
        assertEquals("One task should be run", ONE_TASK, taskCalls)
        verify(onResult).invoke(eq(ONE_TASK))
        assertEquals("Only 1 task should be consumed", TWO_TASKS, runner.size)
    }

    @Test
    fun `run all enqueued tasks`() {
        runner.postQuickTask(task, onResult, onError)
        runner.postQuickTask(task, onResult, onError)

        assertEquals("Queue should contain all enqueued tasks", TWO_TASKS, runner.size)
        val count = runner.runAll()
        assertEquals("Executed tasks should be acknowledged", TWO_TASKS, count)
        verify(task, times(TWO_TASKS)).invoke()
        verify(onResult, times(TWO_TASKS)).invoke(any())
        assertEquals("Entire queue should be processed", EMPTY, runner.size)
    }

    @Test
    fun `run one task when queue is empty`() {
        assertFalse("No task should be run", runner.runOne())
    }

    @Test
    fun `run all tasks when queue is empty`() {
        assertEquals("No task should be run", EMPTY, runner.runAll())
    }

    @Test
    fun `tasks started from callbacks are processed`() {
        val task = { "result" }
        // WHEN
        //      one task is scheduled
        //      task callback schedules another task
        runner.postQuickTask(
            task,
            {
                runner.postQuickTask(
                    task,
                    {
                        runner.postQuickTask(task)
                    }
                )
            }
        )
        assertEquals(ONE_TASK, runner.size)

        // WHEN
        //      runs all
        val count = runner.runAll()

        // THEN
        //      all subsequently scheduled tasks are run too
        assertEquals(THREE_TASKS, count)
    }

    @Test(expected = IllegalStateException::class, timeout = TIMEOUT)
    fun `runner detects infinite loops caused by scheduling tasks recusively`() {
        val recursiveTask: () -> String = object : Function0<String> {
            override fun invoke(): String {
                runner.postQuickTask(this)
                return "result"
            }
        }

        // WHEN
        //      one task is scheduled
        //      task will schedule itself again, causing infinite loop
        runner.postQuickTask(recursiveTask)

        // WHEN
        //      runs all
        runner.runAll()

        // THEN
        //      maximum number of task runs is reached
        //      exception is thrown
    }
}
