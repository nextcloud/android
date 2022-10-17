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
