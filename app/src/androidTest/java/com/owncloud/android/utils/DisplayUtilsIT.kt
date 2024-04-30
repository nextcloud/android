/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayUtilsIT : AbstractIT() {
    @Test
    fun testPixelToDP() {
        val px = 123
        val dp = DisplayUtils.convertPixelToDp(px, targetContext)
        val newPx = DisplayUtils.convertDpToPixel(dp, targetContext)

        assertEquals(px.toLong(), newPx.toLong())
    }
}
