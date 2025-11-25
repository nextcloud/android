/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.adapter.helper.OCFileListAdapterHelper
import com.owncloud.android.utils.FileSortOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCFileListAdapterHelperTest {

    private val context = mockk<Context>(relaxed = true)
    private val helper = OCFileListAdapterHelper()

    private val preferences = mockk<AppPreferences>(relaxed = true)
    private val dataProvider = MockOCFileListAdapterDataProvider()
    private lateinit var directory: OCFile

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        mockkStatic(MainApp::class)
        every { MainApp.getAppContext() } returns context
        every { MainApp.isOnlyPersonFiles() } returns false
        directory = OCFile(OCFile.ROOT_PATH).apply {
            setFolder()
            fileId = 101L
            ownerId = "user123"
            remoteId = "0"
            fileId = 0
        }
    }

    @Test
    fun `prepareFileList dont show hidden files and sort a to z`() = runBlocking {
        val userId = "user123"

        every { preferences.isShowHiddenFilesEnabled() } returns false
        every { preferences.getSortOrderByFolder(directory) } returns FileSortOrder.SORT_A_TO_Z
        every { preferences.isSortFoldersBeforeFiles() } returns true
        every { preferences.isSortFavoritesFirst() } returns false

        val (list, sort) = helper.prepareFileList(
            directory = directory,
            dataProvider = dataProvider,
            onlyOnDevice = false,
            limitToMimeType = "image",
            preferences = preferences,
            userId = userId
        )

        val expected = listOf(
            "favorite.jpg",
            "image.jpg",
            "live.jpg",
            "offline.jpg",
            "personal.jpg",
            "shared.jpg"
        )

        assertEquals(expected, list.map { it.fileName })
        assertEquals(FileSortOrder.SORT_A_TO_Z, sort)
    }
}
