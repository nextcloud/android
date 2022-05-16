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
package com.nextcloud.client.migrations

import android.content.SharedPreferences
import java.lang.UnsupportedOperationException
import java.util.TreeMap

/**
 * This shared preferences implementation uses in-memory value store
 * and it can be used in tests without using global, file-backed storage,
 * improving test isolation.
 *
 * The implementation is not thread-safe.
 */
@Suppress("TooManyFunctions")
class MockSharedPreferences : SharedPreferences {

    class MockEditor(val store: MutableMap<String?, Any?>) : SharedPreferences.Editor {

        val editorStore: MutableMap<String?, Any?> = TreeMap()

        override fun clear(): SharedPreferences.Editor = throw UnsupportedOperationException()

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
            throw UnsupportedOperationException("Implement as needed")

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            editorStore.put(key, value)
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor = throw UnsupportedOperationException()

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            editorStore.put(key, value)
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            editorStore.put(key, values?.toMutableSet())
            return this
        }

        override fun commit(): Boolean = true

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            throw UnsupportedOperationException("Implement as needed")

        override fun apply() = store.putAll(editorStore)

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            editorStore.put(key, value)
            return this
        }
    }

    val store: MutableMap<String?, Any?> = TreeMap()

    override fun contains(key: String?): Boolean = store.containsKey(key)
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = store.getOrDefault(key, defValue) as Boolean

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = throw UnsupportedOperationException()

    override fun getInt(key: String?, defValue: Int): Int = store.getOrDefault(key, defValue) as Int

    override fun getAll(): MutableMap<String?, Any?> {
        return HashMap(store)
    }

    override fun edit(): SharedPreferences.Editor {
        return MockEditor(store)
    }

    override fun getLong(key: String?, defValue: Long): Long {
        throw UnsupportedOperationException()
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        throw UnsupportedOperationException()
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        return store.getOrDefault(key, defValues) as MutableSet<String>?
    }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = throw UnsupportedOperationException()

    override fun getString(key: String?, defValue: String?): String? = store.getOrDefault(key, defValue) as String?
}
