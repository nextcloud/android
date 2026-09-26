/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.res.Resources
import com.owncloud.android.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PassCodeDelayFormatterTest {

    private lateinit var resources: Resources
    private lateinit var sut: PassCodeDelayFormatter

    @Before
    fun setUp() {
        resources = mockk()
        sut = PassCodeDelayFormatter(resources)
    }

    @Test
    fun testGetExplanationTextWhenGivenOneSecondShouldReturnSingularSecondsString() {
        val expected = "Try again in 1 second due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_seconds, 1, 1) } returns expected

        val result = sut.getExplanationText(1)

        assertEquals(expected, result)
    }

    @Test
    fun testGetExplanationTextWhenGivenFiftyNineSecondsShouldReturnPluralSecondsString() {
        val expected = "Try again in 59 seconds due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_seconds, 59, 59) } returns expected

        val result = sut.getExplanationText(59)

        assertEquals(expected, result)
    }

    @Test
    fun testGetExplanationTextWhenGivenSixtySecondsShouldReturnSingularMinutesString() {
        val expected = "Try again in 1 minute due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_minutes, 1, 1) } returns expected

        val result = sut.getExplanationText(60)

        assertEquals(expected, result)
    }

    @Test
    fun testGetExplanationTextWhenGivenOneHundredTwentySecondsShouldReturnPluralMinutesString() {
        val expected = "Try again in 2 minutes due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_minutes, 2, 2) } returns expected

        val result = sut.getExplanationText(120)

        assertEquals(expected, result)
    }

    @Test
    fun testGetExplanationTextWhenGivenNinetySecondsShouldReturnMinutesAndSecondsString() {
        val expected = "Try again in 1 minute and 30 seconds due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_minutes_seconds, 1, 1, 30) } returns expected

        val result = sut.getExplanationText(90)

        assertEquals(expected, result)
    }

    @Test
    fun testGetExplanationTextWhenGivenOneHundredFortyFiveSecondsShouldReturnMinutesAndSecondsString() {
        val expected = "Try again in 2 minutes and 25 seconds due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_minutes_seconds, 2, 2, 25) } returns expected

        val result = sut.getExplanationText(145)

        assertEquals(expected, result)
    }

    @Test
    fun testGetExplanationTextWhenGivenThreeHundredSecondsShouldReturnFiveMinutesString() {
        val expected = "Try again in 5 minutes due to too many wrong attempts"
        every { resources.getQuantityString(R.plurals.passcode_delay_minutes, 5, 5) } returns expected

        val result = sut.getExplanationText(300)

        assertEquals(expected, result)
    }
}
