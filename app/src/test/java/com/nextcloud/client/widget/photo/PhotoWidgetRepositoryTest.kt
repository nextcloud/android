/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PhotoWidgetRepositoryTest {

    @Mock
    private lateinit var preferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    @Mock
    private lateinit var userAccountManager: com.nextcloud.client.account.UserAccountManager

    @Mock
    private lateinit var contentResolver: android.content.ContentResolver

    private lateinit var repository: PhotoWidgetRepository

    companion object {
        private const val WIDGET_ID = 42
        private const val FOLDER_PATH = "/Photos/Vacation"
        private const val ACCOUNT_NAME = "user@nextcloud.example.com"
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.putString(anyString(), anyString())).thenReturn(editor)
        whenever(editor.remove(anyString())).thenReturn(editor)
        repository = PhotoWidgetRepository(preferences, userAccountManager, contentResolver)
    }

    @Test
    fun `saveWidgetConfig stores folder path and account name`() {
        repository.saveWidgetConfig(WIDGET_ID, FOLDER_PATH, ACCOUNT_NAME)

        verify(editor).putString(eq("photo_widget_folder_path_$WIDGET_ID"), eq(FOLDER_PATH))
        verify(editor).putString(eq("photo_widget_account_name_$WIDGET_ID"), eq(ACCOUNT_NAME))
        verify(editor).apply()
    }

    @Test
    fun `getWidgetConfig returns config when both values are present`() {
        whenever(preferences.getString(eq("photo_widget_folder_path_$WIDGET_ID"), eq(null)))
            .thenReturn(FOLDER_PATH)
        whenever(preferences.getString(eq("photo_widget_account_name_$WIDGET_ID"), eq(null)))
            .thenReturn(ACCOUNT_NAME)

        val config = repository.getWidgetConfig(WIDGET_ID)

        assertNotNull(config)
        assertEquals(WIDGET_ID, config!!.widgetId)
        assertEquals(FOLDER_PATH, config.folderPath)
        assertEquals(ACCOUNT_NAME, config.accountName)
    }

    @Test
    fun `getWidgetConfig returns null when folder path is missing`() {
        whenever(preferences.getString(eq("photo_widget_folder_path_$WIDGET_ID"), eq(null)))
            .thenReturn(null)

        val config = repository.getWidgetConfig(WIDGET_ID)

        assertNull(config)
    }

    @Test
    fun `getWidgetConfig returns null when account name is missing`() {
        whenever(preferences.getString(eq("photo_widget_folder_path_$WIDGET_ID"), eq(null)))
            .thenReturn(FOLDER_PATH)
        whenever(preferences.getString(eq("photo_widget_account_name_$WIDGET_ID"), eq(null)))
            .thenReturn(null)

        val config = repository.getWidgetConfig(WIDGET_ID)

        assertNull(config)
    }

    @Test
    fun `deleteWidgetConfig removes both preference keys`() {
        repository.deleteWidgetConfig(WIDGET_ID)

        verify(editor).remove(eq("photo_widget_folder_path_$WIDGET_ID"))
        verify(editor).remove(eq("photo_widget_account_name_$WIDGET_ID"))
        verify(editor).apply()
    }

    @Test
    fun `getRandomImageBitmap returns null when config is missing`() {
        whenever(preferences.getString(eq("photo_widget_folder_path_$WIDGET_ID"), eq(null)))
            .thenReturn(null)

        val bitmap = repository.getRandomImageBitmap(WIDGET_ID)

        assertNull(bitmap)
    }
}
