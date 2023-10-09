/*
 * Nextcloud Android client application
 *
 * @author Alex Plutta
 * Copyright (C) 2019 Alex Plutta
 * Copyright (C) 2019 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter

import com.owncloud.android.lib.resources.activities.model.Activity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ActivityListAdapterTest {
    @Mock
    private val activityListAdapter: ActivityListAdapter? = null
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        MockitoAnnotations.initMocks(activityListAdapter)
        activityListAdapter!!.values = ArrayList()
    }

    @get:Test
    val isHeader__ObjectIsHeader_ReturnTrue: Unit
        get() {
            val header: Any = "Hello"
            val activity: Any = Mockito.mock(Activity::class.java)
            Mockito.`when`(activityListAdapter!!.isHeader(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(0)).thenCallRealMethod()
            activityListAdapter.values.add(header)
            activityListAdapter.values.add(activity)
            val result = activityListAdapter.isHeader(0)
            Assert.assertTrue(result)
        }

    @get:Test
    val isHeader__ObjectIsActivity_ReturnFalse: Unit
        get() {
            val header: Any = "Hello"
            val activity: Any = Mockito.mock(Activity::class.java)
            Mockito.`when`(activityListAdapter!!.isHeader(1)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(1)).thenCallRealMethod()
            activityListAdapter.values.add(header)
            activityListAdapter.values.add(activity)
            Assert.assertFalse(activityListAdapter.isHeader(1))
        }

    @get:Test
    val headerPositionForItem__AdapterIsEmpty_ReturnZero: Unit
        get() {
            Mockito.`when`(activityListAdapter!!.isHeader(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(0)).thenCallRealMethod()
            Assert.assertEquals(0, activityListAdapter.getHeaderPositionForItem(0).toLong())
        }

    @get:Test
    val headerPositionForItem__ItemIsHeader_ReturnCurrentItem: Unit
        get() {
            val header: Any = "Hello"
            val activity: Any = Mockito.mock(Activity::class.java)
            Mockito.`when`(activityListAdapter!!.isHeader(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.isHeader(1)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(1)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.isHeader(2)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(2)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getHeaderPositionForItem(2)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.isHeader(3)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(3)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getHeaderPositionForItem(3)).thenCallRealMethod()
            activityListAdapter.values.add(header)
            activityListAdapter.values.add(activity)
            activityListAdapter.values.add(header)
            activityListAdapter.values.add(activity)
            Assert.assertEquals(2, activityListAdapter.getHeaderPositionForItem(2).toLong())
        }

    @get:Test
    val headerPositionForItem__ItemIsActivity_ReturnNextHeader: Unit
        get() {
            val header: Any = "Hello"
            val activity: Any = Mockito.mock(Activity::class.java)
            Mockito.`when`(activityListAdapter!!.isHeader(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getHeaderPositionForItem(0)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.isHeader(1)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(1)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getHeaderPositionForItem(1)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.isHeader(2)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(2)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getHeaderPositionForItem(2)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.isHeader(3)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getItemViewType(3)).thenCallRealMethod()
            Mockito.`when`(activityListAdapter.getHeaderPositionForItem(3)).thenCallRealMethod()
            activityListAdapter.values.add(header)
            activityListAdapter.values.add(activity)
            activityListAdapter.values.add(header)
            activityListAdapter.values.add(activity)
            Assert.assertEquals(2, activityListAdapter.getHeaderPositionForItem(2).toLong())
        }
}