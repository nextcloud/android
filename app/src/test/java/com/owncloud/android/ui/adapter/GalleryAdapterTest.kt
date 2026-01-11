/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.text.TextUtils
import com.nextcloud.client.account.User
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
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import kotlin.random.Random

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
    fun setUpMocks() {
        mocks = MockitoAnnotations.openMocks(this)

        whenever(transferServiceGetter.storageManager) doReturn storageManager
        whenever(transferServiceGetter.fileUploaderHelper) doReturn fileUploadHelper

        // Mocking TextUtils so OCFile#existsOnDevice() doesn't fail due to Android not being available
        // This is needed so OCFile#toString() works for logging errors
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { arg<String?>(0)?.isEmpty() ?: true }
    }

    @After
    fun tearDownMocks() {
        mocks.close()
    }

    @Test
    fun testItemCount() {
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

        assertEquals(4, sut.getFilesCount())
    }

    @Test
    @Suppress("LongMethod")
    fun testIdUniqueness() {
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
        val rows = mutableListOf<GalleryRow>()

        // Test a known (former) hash collision
        val row1File1 = 263512L
        val row1File2 = 148830L
        val row2File1 = 279897L
        val row2File2 = 132445L
        rows.add(
            GalleryRow(
                listOf(
                    OCFile("/$row1File1.md").apply {
                        fileId = row1File1
                        parentId = 0
                    },
                    OCFile("/$row1File2.md").apply {
                        fileId = row1File2
                        parentId = 0
                    }
                ),
                thumbnailSize,
                thumbnailSize
            )
        )
        rows.add(
            GalleryRow(
                listOf(
                    OCFile("/$row2File1.md").apply {
                        fileId = row2File1
                        parentId = 0
                    },
                    OCFile("/$row2File2.md").apply {
                        fileId = row2File2
                        parentId = 0
                    }
                ),
                thumbnailSize,
                thumbnailSize
            )
        )
        val alreadyUsedFileIds = listOf(row1File1, row1File2, row2File1, row2File2)

        // Generate some random Ids for some explorative testing
        val randomFileIds = uniquePositiveRandomLongs(10000, 1000000)
        for (i in 0..<randomFileIds.size step 2) {
            val id1 = randomFileIds[i]
            val id2 = randomFileIds[i + 1]
            if (id1 in alreadyUsedFileIds || id2 in alreadyUsedFileIds) {
                continue
            }
            rows.add(
                GalleryRow(
                    listOf(
                        OCFile("/$id1.md").apply { fileId = id1 },
                        OCFile("/$id2.md").apply { fileId = id2 }
                    ),
                    thumbnailSize,
                    thumbnailSize
                )
            )
        }

        val list = listOf(GalleryItems(1649317247, rows))
        sut.addFiles(list)

        val itemIds = mutableMapOf<Long, GalleryRow>()
        for (i in 0..<sut.sectionCount) {
            for (j in 0..<list[i].rows.size) {
                val itemId = sut.getItemId(i, j)

                assertFalse(
                    "Item IDs are not unique: $itemId, in section $i, row $j is already taken by ${itemIds[itemId]}",
                    itemIds.contains(itemId)
                )
                itemIds[itemId] = list[i].rows[j]
            }
        }
        assertEquals("Exactly one section required, else the next assert is imprecise", 1, sut.sectionCount)
        assertEquals("The duplicate detection seems to be faulty", rows.size, itemIds.size)
    }

    fun uniquePositiveRandomLongs(count: Int, max: Long = Long.MAX_VALUE): List<Long> {
        require(count >= 0) { "count must be non-negative" }
        if (count == 0) return emptyList()

        val set = HashSet<Long>(count)
        while (set.size < count) {
            // produce positive (> 0) values
            set.add(Random.nextLong(1, max))
        }
        return set.toList()
    }
}
