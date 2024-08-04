/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelTest {

    @Test
    fun `parsing level tag`() {
        Level.values().forEach {
            val parsed = Level.fromTag(it.tag)
            assertEquals(parsed, it)
        }
    }

    @Test
    fun `level parser handles unkown values`() {
        assertEquals(Level.UNKNOWN, Level.fromTag("non-existing-tag"))
    }
}
