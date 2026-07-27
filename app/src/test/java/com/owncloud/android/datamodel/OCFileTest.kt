/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OCFileTest {
    @Test
    fun testShareesDefaultsToEmptyMutableList() {
        val sut = OCFile("/")

        assertTrue(sut.sharees.isEmpty())

        sut.sharees.add(ShareeUser("alice", "Alice", ShareType.USER))
        assertEquals(1, sut.sharees.size)
    }

    @Test
    fun testShareesStayMutableWhenSetFromImmutableList() {
        val sut = OCFile("/")

        sut.sharees = listOf(ShareeUser("alice", "Alice", ShareType.USER))

        sut.sharees.add(ShareeUser("bob", "Bob", ShareType.USER))
        assertEquals(2, sut.sharees.size)
    }

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
