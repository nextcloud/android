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
package com.nextcloud.sso

import com.nextcloud.android.sso.InputStreamBinder
import com.nextcloud.android.sso.QueryParam
import org.junit.Assert.assertEquals
import org.junit.Test

class InputStreamBinderTest {
    @Test
    fun convertMapToNVP() {
        val source = mutableMapOf<String, String>()
        source["quality"] = "1024p"
        source["someOtherParameter"] = "parameterValue"
        source["duplicate"] = "1"
        source["duplicate"] = "2" // this overwrites previous parameter

        val output = InputStreamBinder.convertMapToNVP(source)

        assertEquals(source.size, output.size)
        assertEquals("1024p", output[0].value)
        assertEquals("parameterValue", output[1].value)
        assertEquals("2", output[2].value)
    }

    @Test
    fun convertListToNVP() {
        val source = mutableListOf<QueryParam>()
        source.add(QueryParam("quality", "1024p"))
        source.add(QueryParam("someOtherParameter", "parameterValue"))
        source.add(QueryParam("duplicate", "1"))
        source.add(QueryParam("duplicate", "2")) // here we can have same parameter multiple times

        val output = InputStreamBinder.convertListToNVP(source)

        assertEquals(source.size, output.size)
        assertEquals("1024p", output[0].value)
        assertEquals("parameterValue", output[1].value)
        assertEquals("1", output[2].value)
        assertEquals("2", output[3].value)
    }
}
