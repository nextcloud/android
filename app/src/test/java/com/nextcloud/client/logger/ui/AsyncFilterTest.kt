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
package com.nextcloud.client.logger.ui

import com.nextcloud.client.core.ManualAsyncRunner
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@Suppress("MagicNumber") // numbers are used for testing sort
class AsyncFilterTest {

    class OnResult<T> : (List<T>, Long) -> Unit {
        var arg: List<T>? = null
        var dt: Long? = null
        override fun invoke(arg: List<T>, dt: Long) {
            this.arg = arg
            this.dt = dt
        }
    }

    private lateinit var time: () -> Long
    private lateinit var runner: ManualAsyncRunner
    private lateinit var filter: AsyncFilter

    @Before
    fun setUp() {
        time = mock()
        whenever(time.invoke()).thenReturn(System.currentTimeMillis())
        runner = ManualAsyncRunner()
        filter = AsyncFilter(runner, time)
    }

    @Test
    fun `collection is filtered asynchronously`() {
        val collection = listOf(1, 2, 3, 4, 5, 6)
        val predicate = { arg: Int -> arg > 3 }
        val result = OnResult<Int>()

        // GIVEN
        //      filtering is scheduled
        filter.filter(collection, predicate, result)
        assertEquals(1, runner.size)
        assertNull(result.arg)

        // WHEN
        //      task completes
        runner.runOne()

        // THEN
        //      result is delivered via callback
        assertEquals(listOf(4, 5, 6), result.arg)
    }

    @Test
    fun `filtering request is enqueued if one already running`() {
        val collection = listOf(1, 2, 3)
        val firstPredicate = { arg: Int -> arg > 1 }
        val secondPredicate = { arg: Int -> arg > 2 }
        val firstResult = OnResult<Int>()
        val secondResult = OnResult<Int>()

        // GIVEN
        //      filtering task is already running

        filter.filter(collection, firstPredicate, firstResult)
        assertEquals(1, runner.size)

        // WHEN
        //      new filtering is requested
        //      first filtering task completes
        filter.filter(collection, secondPredicate, secondResult)
        runner.runOne()

        // THEN
        //      first filtering task result is delivered
        //      second filtering task is scheduled immediately
        //      second filtering result will be delivered when completes
        assertEquals(listOf(2, 3), firstResult.arg)
        assertEquals(1, runner.size)

        runner.runOne()
        assertEquals(listOf(3), secondResult.arg)
        assertEquals(0, runner.size)
    }

    @Test
    fun `pending requests are overwritten by new requests`() {
        val collection = listOf(1, 2, 3, 4, 5, 6)

        val firstPredicate = { arg: Int -> arg > 1 }
        val firstResult = OnResult<Int>()

        val secondPredicate: (Int) -> Boolean = mock()
        whenever(secondPredicate.invoke(any())).thenReturn(false)
        val secondResult = OnResult<Int>()

        val thirdPredicate = { arg: Int -> arg > 3 }
        val thirdResult = OnResult<Int>()

        // GIVEN
        //      filtering task is already running
        filter.filter(collection, firstPredicate, firstResult)
        assertEquals(1, runner.size)

        // WHEN
        //      few new filtering requests are enqueued
        //      first filtering task completes
        filter.filter(collection, secondPredicate, secondResult)
        filter.filter(collection, thirdPredicate, thirdResult)
        runner.runOne()
        assertEquals(1, runner.size)
        runner.runOne()

        // THEN
        //      second filtering task is overwritten
        //      second filtering task never runs
        //      third filtering task runs and completes
        //      no new tasks are scheduled
        verify(secondPredicate, never()).invoke(any())
        assertNull(secondResult.arg)
        assertEquals(listOf(4, 5, 6), thirdResult.arg)
        assertEquals(0, runner.size)
    }

    @Test
    fun `filtering is timed`() {
        // GIVEN
        //      filtering operation is scheduled
        val startTime = System.currentTimeMillis()
        whenever(time.invoke()).thenReturn(startTime)
        val result = OnResult<Int>()
        filter.filter(listOf(1, 2, 3), { true }, result)

        // WHEN
        //      result is delivered with a delay
        val delayMs = 123L
        whenever(time.invoke()).thenReturn(startTime + delayMs)
        runner.runAll()

        // THEN
        //      delay is calculated from current time
        assertEquals(result.dt, delayMs)
    }
}
