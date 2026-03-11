/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.ui.activities.adapter.ActivityListAdapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class ActivityListAdapterTest {

    @Mock
    private lateinit var adapter: ActivityListAdapter

    private val header: Any = "Hello"
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activity = mock(Activity::class.java)
        adapter.values.clear()
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
    fun `getHeaderPositionForItem returns 0 when adapter is empty`() {
        assertEquals(0, adapter.getHeaderPositionForItem(0))
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
