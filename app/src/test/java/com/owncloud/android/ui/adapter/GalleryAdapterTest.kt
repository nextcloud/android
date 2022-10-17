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

package com.owncloud.android.ui.adapter

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.theme.ViewThemeUtils
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

class GalleryAdapterTest {
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var user: User

    @Mock
    lateinit var ocFileListFragmentInterface: OCFileListFragmentInterface

    @Mock
    lateinit var preferences: AppPreferences

    @Mock
    lateinit var transferServiceGetter: ComponentsGetter

    @Mock
    lateinit var storageManager: FileDataStorageManager

    @Mock
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var mocks: AutoCloseable

    @Before
    fun setUp() {
        mocks = MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        mocks.close()
    }

    @Test
    fun testItemCount() {
        whenever(transferServiceGetter.storageManager) doReturn storageManager
        val thumbnailSize = 50

        val sut = GalleryAdapter(
            context,
            user,
            ocFileListFragmentInterface,
            preferences,
            transferServiceGetter,
            viewThemeUtils,
            5,
            thumbnailSize
        )

        val list = listOf(
            GalleryItems(
                1649317247,
                listOf(GalleryRow(listOf(OCFile("/1.md"), OCFile("/2.md")), thumbnailSize, thumbnailSize))
            ),
            GalleryItems(
                1649317248,
                listOf(GalleryRow(listOf(OCFile("/1.md"), OCFile("/2.md")), thumbnailSize, thumbnailSize))
            )
        )

        sut.addFiles(list)

        assertEquals(2, sut.getFilesCount())
    }
}
