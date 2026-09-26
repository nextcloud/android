/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.view

import com.owncloud.android.AbstractIT
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenMetricsIT : AbstractIT() {
    @Test
    fun testPixelToDP() {
        val px = 123
        val dp = ScreenMetrics.pxToDp(px, targetContext)
        val newPx = ScreenMetrics.dpToPx(dp, targetContext)

        assertEquals(px.toLong(), newPx.toLong())
    }
}
