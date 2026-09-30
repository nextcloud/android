/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.text

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkFormatterTest {

    @Test
    fun testConvertIdn() {
        assertEquals("", LinkFormatter.toAsciiDomain(""))
        assertEquals("", LinkFormatter.toUnicodeDomain(""))
        assertEquals("http://www.nextcloud.com", LinkFormatter.toAsciiDomain("http://www.nextcloud.com"))
        assertEquals("http://www.xn--wlkchen-90a.com", LinkFormatter.toAsciiDomain("http://www.wölkchen.com"))
        assertEquals("http://www.wölkchen.com", LinkFormatter.toUnicodeDomain("http://www.xn--wlkchen-90a.com"))
    }
}
