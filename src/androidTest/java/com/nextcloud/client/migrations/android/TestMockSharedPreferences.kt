/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.migrations.android

import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue

@Suppress("MagicNumber", "FunctionNaming")
class TestMockSharedPreferences {

    private lateinit var mock: MockSharedPreferences

    @Before
    fun setUp() {
        mock = MockSharedPreferences()
    }

    @Test
    fun get_set_string_set() {
        val value = setOf("alpha", "bravo", "charlie")
        mock.edit().putStringSet("key", value).apply()
        val copy = mock.getStringSet("key", mutableSetOf())
        assertNotSame(value, copy)
        assertEquals(value, copy)
    }

    @Test
    fun get_set_int() {
        val value = 42
        val editor = mock.edit()
        editor.putInt("key", value)
        assertEquals(100, mock.getInt("key", 100))
        editor.apply()
        assertEquals(42, mock.getInt("key", 100))
    }

    @Test
    fun get_set_boolean() {
        val value = true
        val editor = mock.edit()
        editor.putBoolean("key", value)
        assertFalse(mock.getBoolean("key", false))
        editor.apply()
        assertTrue(mock.getBoolean("key", false))
    }

    @Test
    fun get_set_string() {
        val value = "a value"
        val editor = mock.edit()
        editor.putString("key", value)
        assertEquals("default", mock.getString("key", "default"))
        editor.apply()
        assertEquals("a value", mock.getString("key", "default"))
    }
}
