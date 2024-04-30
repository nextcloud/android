/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OCFileTest {
    @Test
    fun testLongIds() {
        val sut = OCFile("/")

        sut.remoteId = "12345678ocjycgrudn78"
        assertEquals(12345678, sut.localId)

        sut.remoteId = "00000008ocjycgrudn78"
        assertEquals(8, sut.localId)

        // this will fail as fileId is too large
        sut.remoteId = "1234567891011ocjycgrudn78"
        assertNotEquals(1234567891011L, sut.localId)

        sut.localId = 1234567891011L
        assertEquals(1234567891011L, sut.localId)
    }
}
