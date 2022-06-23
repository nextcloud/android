/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2022 TSI-mc
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.owncloud.android.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateExtensionsTest {

    @Test
    fun isCurrentYear_checkForAllConditions() {
        assertFalse(System.currentTimeMillis().isCurrentYear(""))
        assertFalse(System.currentTimeMillis().isCurrentYear(null))

        val year2022TimeMills = 1652892268000L
        assertTrue(year2022TimeMills.isCurrentYear("2022"))

        assertFalse(year2022TimeMills.isCurrentYear("2021"))
    }

    @Test
    fun getFormattedStringDate_checkForAllConditions() {
        val year2022TimeMills = 1652892268000L
        val actualYearValue = year2022TimeMills.getFormattedStringDate("yyyy")
        assertTrue(actualYearValue == "2022")
        assertFalse(actualYearValue == "2021")
        assertFalse(actualYearValue == "")

        val actualYearNewValue = year2022TimeMills.getFormattedStringDate("")
        assertTrue(actualYearNewValue == "")
        assertFalse(actualYearNewValue == "2022")
    }
}
