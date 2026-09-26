/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UploadResultTest {

    @Test
    fun skippedRoundTripsThroughStoredValue() {
        val stored = UploadResult.SKIPPED.value
        assertEquals(UploadResult.SKIPPED, UploadResult.fromValue(stored))
    }

    @Test
    fun skippedIsDistinctFromUploaded() {
        assertNotEquals(UploadResult.SKIPPED.value, UploadResult.UPLOADED.value)
        assertEquals(UploadResult.UPLOADED, UploadResult.fromValue(UploadResult.UPLOADED.value))
    }
}
