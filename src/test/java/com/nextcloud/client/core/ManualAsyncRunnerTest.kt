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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class ManualAsyncRunnerTest {

    private lateinit var runner: ManualAsyncRunner

    @Mock
    private lateinit var task: () -> Int

    @Mock
    private lateinit var onResult: OnResultCallback<Int>

    @Mock
    private lateinit var onError: OnErrorCallback

    private var taskCalls: Int = 0

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        runner = ManualAsyncRunner()
        taskCalls = 0
        whenever(task.invoke()).thenAnswer { taskCalls++; taskCalls }
    }

    @Test
    fun `tasks are queued`() {
        assertEquals(0, runner.size)
        runner.post(task, onResult, onError)
        runner.post(task, onResult, onError)
        runner.post(task, onResult, onError)
        assertEquals("Expected 3 tasks to be enqueued", 3, runner.size)
    }

    @Test
    fun `run one enqueued task`() {
        runner.post(task, onResult, onError)
        runner.post(task, onResult, onError)
        runner.post(task, onResult, onError)

        assertEquals("Queue should contain all enqueued tasks", 3, runner.size)
        val run = runner.runOne()
        assertTrue("Executed task should be acknowledged", run)
        assertEquals("One task should be run", 1, taskCalls)
        verify(onResult).invoke(eq(1))
        assertEquals("Only 1 task should be consumed", 2, runner.size)
    }

    @Test
    fun `run all enqueued tasks`() {
        runner.post(task, onResult, onError)
        runner.post(task, onResult, onError)

        assertEquals("Queue should contain all enqueued tasks", 2, runner.size)
        val count = runner.runAll()
        assertEquals("Executed tasks should be acknowledged", 2, count)
        verify(task, times(2)).invoke()
        verify(onResult, times(2)).invoke(any())
        assertEquals("Entire queue should be processed", 0, runner.size)
    }

    @Test
    fun `run one task when queue is empty`() {
        assertFalse("No task should be run", runner.runOne())
    }

    @Test
    fun `run all tasks when queue is empty`() {
        assertEquals("No task should be run", 0, runner.runAll())
    }

    @Test
    fun `tasks started from callbacks are processed`() {
        val task = { "result" }
        // WHEN
        //      one task is scheduled
        //      task callback schedules another task
        runner.post(task, {
            runner.post(task, {
                runner.post(task)
            })
        })
        assertEquals(1, runner.size)

        // WHEN
        //      runs all
        val count = runner.runAll()

        // THEN
        //      all subsequently scheduled tasks are run too
        assertEquals(3, count)
    }

    @Test(expected = IllegalStateException::class, timeout = 10000)
    fun `runner detects infinite loops caused by scheduling tasks recusively`() {
        val recursiveTask: () -> String = object : Function0<String> {
            override fun invoke(): String {
                runner.post(this)
                return "result"
            }
        }

        // WHEN
        //      one task is scheduled
        //      task will schedule itself again, causing infinite loop
        runner.post(recursiveTask)

        // WHEN
        //      runs all
        runner.runAll()

        // THEN
        //      maximum number of task runs is reached
        //      exception is thrown
    }
}
