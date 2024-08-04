/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TaskTest {

    @Mock
    private lateinit var taskBody: TaskFunction<String, Int>

    @Mock
    private lateinit var removeFromQueue: (Runnable) -> Boolean

    @Mock
    private lateinit var onResult: OnResultCallback<String>

    @Mock
    private lateinit var onError: OnErrorCallback

    @Mock
    private lateinit var onProgress: OnProgressCallback<Int>

    private lateinit var task: Task<String, Int>

    fun post(r: Runnable): Boolean {
        r.run()
        return true
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val postResult = { r: Runnable ->
            r.run()
            true
        }
        task = Task(this::post, removeFromQueue, taskBody, onResult, onError, onProgress)
    }

    @Test
    fun `task result is posted`() {
        whenever(taskBody.invoke(any(), any())).thenReturn("result")
        task.run()
        verify(onResult).invoke(eq("result"))
        verify(onError, never()).invoke(any())
    }

    @Test
    fun `task result is not posted when cancelled`() {
        whenever(taskBody.invoke(any(), any())).thenReturn("result")
        task.cancel()
        task.run()
        verify(onResult, never()).invoke(any())
        verify(onError, never()).invoke(any())
    }

    @Test
    fun `task error is posted`() {
        val exception = RuntimeException("")
        whenever(taskBody.invoke(any(), any())).thenThrow(exception)
        task.run()
        verify(onResult, never()).invoke(any())
        verify(onError).invoke(same(exception))
    }

    @Test
    fun `task error is not posted when cancelled`() {
        val exception = RuntimeException("")
        whenever(taskBody.invoke(any(), any())).thenThrow(exception)
        task.cancel()
        task.run()
        verify(onResult, never()).invoke(any())
        verify(onError, never()).invoke(any())
    }
}
