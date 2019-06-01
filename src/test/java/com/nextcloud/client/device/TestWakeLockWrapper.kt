/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.device

import android.os.PowerManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException

class TestWakeLockWrapper {

    internal lateinit var nativeLock: PowerManager.WakeLock
    internal lateinit var wrapper: WakeLockWrapper
    internal lateinit var emptyWrapper: WakeLockWrapper

    @Before
    fun setUp() {
        nativeLock = mock()
        wrapper = WakeLockWrapper(nativeLock)
        emptyWrapper = WakeLockWrapper(null)
    }

    @Test
    fun `wake lock wrapper delegates status check to native lock`() {
        // GIVEN
        //      native lock is held
        whenever(nativeLock.isHeld).thenReturn(true)

        // WHEN
        //      lock wrapper is checked
        val status = wrapper.isHeld

        // THEN
        //      native lock status is retrieved
        assertTrue(status)
        verify(nativeLock).isHeld
    }

    @Test
    fun `empty wake lock wrapper is not held`() {
        // GIVEN
        //      wake lock wrapper is empty (no native lock)
        val emptyLock = WakeLockWrapper(null)
        assertNull(emptyLock.lock)

        // WHEN
        //      lock status is checked
        val status = emptyLock.isHeld

        // THEN
        //      lock status is false
        //      native lock is null
        assertFalse(status)
    }

    @Test
    fun `lock is released after runnable`() {
        // GIVEN
        //      lock wrapper is not empty
        assertNotNull(wrapper.lock)

        // WHEN
        //      block of code is run on a lock
        var called = false
        wrapper.runAndRelease(Runnable {
            called = true
        })

        // THEN
        //      block is invoked
        //      native lock is released
        assertTrue(called)
        verify(wrapper.lock!!).release()
    }

    @Test
    fun `lock is released after runnable throws exception`() {
        // GIVEN
        //      lock wrapper is not empty
        assertNotNull(wrapper.lock)

        // WHEN
        //      block of code is run on a lock
        //      runnable throws
        var exceptionThrown = false
        try {
            wrapper.runAndRelease(Runnable {
                throw RuntimeException("dummy")
            })
        } catch (ex: RuntimeException) {
            exceptionThrown = true
        }

        // THEN
        //      exception is not consumed
        //      lock is released
        assertTrue(exceptionThrown)
        verify(wrapper.lock!!).release()
    }

    @Test
    fun `can run block on empty wake lock wrapper`() {
        // GIVEN
        //      lock wrapper is empty
        assertNull(emptyWrapper.lock)

        // WHEN
        //      block of code is run on a lock
        var called = false
        wrapper.runAndRelease(Runnable {
            called = true
        })

        // THEN
        //      block is invoked
        //      no NPE crash
        assertTrue(called)
    }
}
