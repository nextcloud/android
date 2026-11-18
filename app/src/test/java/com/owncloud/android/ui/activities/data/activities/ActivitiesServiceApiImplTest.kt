/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activities.data.activities

import com.owncloud.android.lib.resources.activities.GetActivitiesRemoteOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitiesServiceApiImplTest {

    private fun GetActivitiesRemoteOperation.longField(name: String): Long =
        GetActivitiesRemoteOperation::class.java.getDeclaredField(name).let {
            it.isAccessible = true
            it.getLong(this)
        }

    @Test
    fun firstPageHasNoFilter() {
        val operation = ActivitiesServiceApiImpl.createOperation(0)

        assertEquals(-1L, operation.longField("fileId"))
        assertEquals(-1L, operation.longField("lastGiven"))
    }

    @Test
    fun nextPageKeepsTheFullActivityIdWithoutFilteringByFile() {
        val operation = ActivitiesServiceApiImpl.createOperation(SNOWFLAKE_ACTIVITY_ID)

        assertEquals(-1L, operation.longField("fileId"))
        assertEquals(SNOWFLAKE_ACTIVITY_ID, operation.longField("lastGiven"))
    }

    companion object {
        // Snowflake IDs need more than 32 bits
        private const val SNOWFLAKE_ACTIVITY_ID = 130_000_000_000_000_123L
    }
}
