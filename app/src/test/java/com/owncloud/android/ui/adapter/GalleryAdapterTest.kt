/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.theme.ViewThemeUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
    lateinit var fileUploadHelper: FileUploadHelper

    @Mock
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var mocks: AutoCloseable

    @Before
    fun setUp() {
        mocks = MockitoAnnotations.openMocks(this)
        mockkObject(FileDownloadHelper.Companion)
        every { FileDownloadHelper.instance() } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        mocks.close()
    }

    @Test
    fun testItemCount() {
        whenever(transferServiceGetter.storageManager) doReturn storageManager
        whenever(transferServiceGetter.fileUploaderHelper) doReturn fileUploadHelper

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
