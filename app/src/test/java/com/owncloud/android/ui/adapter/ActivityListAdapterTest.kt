/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.ui.activities.adapter.ActivityListAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class ActivityListAdapterTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private lateinit var adapter: ActivityListAdapter

    private val header: Any = "Hello"
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activity = mock(Activity::class.java)

        ActivityListAdapter::class.java
            .getDeclaredField("values")
            .apply { isAccessible = true }
            .set(adapter, mutableListOf<Any>())
    }

    @Test
    fun `isHeader returns true when item is a String header`() {
        adapter.values.addAll(listOf(header, activity))
        assertTrue(adapter.isHeader(0))
    }

    @Test
    fun `isHeader returns false when item is an Activity`() {
        adapter.values.addAll(listOf(header, activity))
        assertFalse(adapter.isHeader(1))
    }

    @Test
    fun `getHeaderPositionForItem returns -1 when adapter is empty`() {
        assertEquals(-1, adapter.getHeaderPositionForItem(0))
    }

    @Test
    fun `getHeaderPositionForItem returns current position when item is a header`() {
        adapter.values.addAll(listOf(header, activity, header, activity))
        assertEquals(2, adapter.getHeaderPositionForItem(2))
    }

    @Test
    fun `getHeaderPositionForItem returns preceding header position when item is an Activity`() {
        adapter.values.addAll(listOf(header, activity, header, activity))
        assertEquals(2, adapter.getHeaderPositionForItem(3))
    }
}
