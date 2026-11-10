/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.preferences

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DebugLoggingPreferencesTest {
    @Test
    fun defaultsToDisabledAndPersistsBothSwitchStates() {
        val values = mutableMapOf<String, Boolean>()
        val sharedPreferences: SharedPreferences = mock()
        val editor: SharedPreferences.Editor = mock()
        whenever(sharedPreferences.getBoolean(any(), any())).thenAnswer {
            values[it.getArgument<String>(0)] ?: it.getArgument<Boolean>(1)
        }
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenAnswer {
            values[it.getArgument(0)] = it.getArgument(1)
            editor
        }

        val preferences = AppPreferencesImpl(mock(), sharedPreferences, mock())
        assertFalse(preferences.isDebugLoggingEnabled)
        preferences.setDebugLoggingEnabled(true)
        assertTrue(AppPreferencesImpl(mock(), sharedPreferences, mock()).isDebugLoggingEnabled)
        preferences.setDebugLoggingEnabled(false)
        assertFalse(AppPreferencesImpl(mock(), sharedPreferences, mock()).isDebugLoggingEnabled)
    }
}
