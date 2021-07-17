/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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
package com.owncloud.android.authentication

import junit.framework.Assert.assertEquals
import org.junit.Test

class AuthenticatorUrlUtilsTest {
    @Test
    fun test() {
        val expected = "https://10.0.2.2/nc"
        val urls = arrayOf(
            "https://10.0.2.2/nc/",
            "https://10.0.2.2/nc/remote.php/webdav/",
            "https://10.0.2.2/nc/remote.php/dav/",
            "https://10.0.2.2/nc/remote.php",
            "https://10.0.2.2/nc/remote.php/"
        )

        for (url in urls) {
            assertEquals(expected, AuthenticatorUrlUtils.trimWebdavSuffix(url))
            assertEquals(expected, AuthenticatorUrlUtils.normalizeUrlSuffix(url))
        }

        assertEquals("", AuthenticatorUrlUtils.trimWebdavSuffix(null))
        assertEquals("", AuthenticatorUrlUtils.normalizeUrlSuffix(null))
    }
}
