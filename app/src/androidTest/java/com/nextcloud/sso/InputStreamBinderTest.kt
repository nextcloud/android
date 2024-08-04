/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
