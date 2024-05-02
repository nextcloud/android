/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Akshay Chaurasia <akshay.chaurasia@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.utils

import com.nextcloud.utils.extensions.getFormattedStringDate
import com.nextcloud.utils.extensions.isCurrentYear
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

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
