/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
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
