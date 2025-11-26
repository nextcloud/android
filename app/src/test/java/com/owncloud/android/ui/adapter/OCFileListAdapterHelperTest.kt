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
import com.owncloud.android.utils.MimeType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("LongMethod", "LongParameterList")
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
    fun `prepareFileList with multiple folders and sort z to a`() = runBlocking {
        val userId = "user123"
        val directory = OCFile(OCFile.ROOT_PATH).apply {
            setFolder()
            fileId = 101L
            ownerId = "user123"
            remoteId = "0"
            fileId = 0
        }
        val subDirectory = OCFile("/subDir").apply {
            setFolder()
            fileId = 102L
            ownerId = "user123"
            remoteId = "1"
            fileId = 1
            parentId = 0
        }
        val subDirectory2 = OCFile("/subDir2").apply {
            setFolder()
            fileId = 103L
            ownerId = "user123"
            remoteId = "2"
            fileId = 2
            parentId = 0
        }
        val file1 = createTestOCFile(
            directory.fileId,
            "/image.jpg",
            11,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/image.jpg"
        )
        val file2 = createTestOCFile(
            directory.fileId,
            "/video.mp4",
            12,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )
        val subFile1 = createTestOCFile(
            subDirectory.fileId,
            "/video2.mp4",
            21,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )

        val files =
            listOf(directory, subDirectory, subDirectory2, file1, file2, subFile1)
        dataProvider.setEntities(files)

        every { preferences.isShowHiddenFilesEnabled() } returns false
        every { preferences.getSortOrderByFolder(directory) } returns FileSortOrder.SORT_Z_TO_A
        every { preferences.isSortFoldersBeforeFiles() } returns true
        every { preferences.isSortFavoritesFirst() } returns true

        val (list, sort) = helper.prepareFileList(
            directory = directory,
            dataProvider = dataProvider,
            onlyOnDevice = false,
            limitToMimeType = "",
            preferences = preferences,
            userId = userId
        )

        val expected = listOf(
            "subDir2",
            "subDir",
            "video.mp4",
            "image.jpg"
        )

        assertEquals(expected, list.map { it.fileName })
        assertEquals(FileSortOrder.SORT_Z_TO_A, sort)
    }

    @Test
    fun `prepareFileList with multiple folders and favorites firsts`() = runBlocking {
        val userId = "user123"
        val directory = OCFile(OCFile.ROOT_PATH).apply {
            setFolder()
            fileId = 101L
            ownerId = "user123"
            remoteId = "0"
            fileId = 0
        }
        val subDirectory = OCFile("/subDir").apply {
            setFolder()
            fileId = 102L
            ownerId = "user123"
            remoteId = "1"
            fileId = 1
            parentId = 0
        }
        val subDirectory2 = OCFile("/subDir2").apply {
            setFolder()
            fileId = 103L
            ownerId = "user123"
            remoteId = "2"
            fileId = 2
            parentId = 0
        }
        val file1 = createTestOCFile(
            directory.fileId,
            "/image.jpg",
            11,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/image.jpg"
        )
        val file2 = createTestOCFile(
            directory.fileId,
            "/video.mp4",
            12,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )
        val subFile1 = createTestOCFile(
            subDirectory.fileId,
            "/video2.mp4",
            21,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )
        val file3 = createTestOCFile(
            directory.fileId,
            "/fav_image.jpg",
            19,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/image9.jpg",
            isFavorite = true
        )

        val files =
            listOf(directory, subDirectory, subDirectory2, file1, file2, subFile1, file3)
        dataProvider.setEntities(files)

        every { preferences.isShowHiddenFilesEnabled() } returns false
        every { preferences.getSortOrderByFolder(directory) } returns FileSortOrder.SORT_A_TO_Z
        every { preferences.isSortFoldersBeforeFiles() } returns true
        every { preferences.isSortFavoritesFirst() } returns true

        val (list, sort) = helper.prepareFileList(
            directory = directory,
            dataProvider = dataProvider,
            onlyOnDevice = false,
            limitToMimeType = "",
            preferences = preferences,
            userId = userId
        )

        val expected = listOf(
            "fav_image.jpg",
            "subDir",
            "subDir2",
            "image.jpg",
            "video.mp4"
        )

        assertEquals(expected, list.map { it.fileName })
        assertEquals(FileSortOrder.SORT_A_TO_Z, sort)
    }

    @Test
    fun `prepareFileList with multiple folders`() = runBlocking {
        val userId = "user123"
        val directory = OCFile(OCFile.ROOT_PATH).apply {
            setFolder()
            fileId = 101L
            ownerId = "user123"
            remoteId = "0"
            fileId = 0
        }
        val subDirectory = OCFile("/subDir").apply {
            setFolder()
            fileId = 102L
            ownerId = "user123"
            remoteId = "1"
            fileId = 1
            parentId = 0
        }
        val subDirectory2 = OCFile("/subDir2").apply {
            setFolder()
            fileId = 103L
            ownerId = "user123"
            remoteId = "2"
            fileId = 2
            parentId = 0
        }
        val file1 = createTestOCFile(
            directory.fileId,
            "/image.jpg",
            11,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/image.jpg"
        )
        val file2 = createTestOCFile(
            directory.fileId,
            "/video.mp4",
            12,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )
        val subFile1 = createTestOCFile(
            subDirectory.fileId,
            "/video2.mp4",
            21,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )

        val files =
            listOf(directory, subDirectory, subDirectory2, file1, file2, subFile1)
        dataProvider.setEntities(files)

        every { preferences.isShowHiddenFilesEnabled() } returns false
        every { preferences.getSortOrderByFolder(directory) } returns FileSortOrder.SORT_A_TO_Z
        every { preferences.isSortFoldersBeforeFiles() } returns true
        every { preferences.isSortFavoritesFirst() } returns false

        val (list, sort) = helper.prepareFileList(
            directory = directory,
            dataProvider = dataProvider,
            onlyOnDevice = false,
            limitToMimeType = "",
            preferences = preferences,
            userId = userId
        )

        val expected = listOf(
            "subDir",
            "subDir2",
            "image.jpg",
            "video.mp4"
        )

        assertEquals(expected, list.map { it.fileName })
        assertEquals(FileSortOrder.SORT_A_TO_Z, sort)
    }

    @Test
    fun `prepareFileList dont show hidden files and sort a to z`() = runBlocking {
        val userId = "user123"

        val directory = OCFile(OCFile.ROOT_PATH).apply {
            setFolder()
            fileId = 101L
            ownerId = "user123"
            remoteId = "0"
            fileId = 0
        }
        val hidden = createTestOCFile(
            directory.fileId,
            "/.hidden.jpg",
            1,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            isHidden = true,
            localPath = "/local/hidden.jpg"
        )
        val image = createTestOCFile(
            directory.fileId,
            "/image.jpg",
            2,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/image.jpg"
        )
        val video = createTestOCFile(
            directory.fileId,
            "/video.mp4",
            3,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/video.mp4"
        )
        val temp = createTestOCFile(
            directory.fileId,
            "/temp.tmp",
            4,
            ownerId = "user123",
            mimeType = MimeType.FILE,
            localPath = "/local/temp.tmp"
        )
        val otherUsersFile =
            createTestOCFile(
                202,
                "/other.jpg",
                5,
                ownerId = "x",
                mimeType = MimeType.JPEG,
                localPath = "/local/other.jpg"
            )
        val personal = createTestOCFile(
            directory.fileId,
            "/personal.jpg",
            6,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/personal.jpg"
        )
        val shared = createTestOCFile(
            directory.fileId,
            "/shared.jpg",
            7,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            isSharedViaLink = true,
            localPath = "/local/shared.jpg"
        )
        val favorite = createTestOCFile(
            directory.fileId,
            "/favorite.jpg",
            8,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            isFavorite = true,
            localPath = "/local/favorite.jpg"
        )
        val livePhotoImg = createTestOCFile(
            directory.fileId,
            "/live.jpg",
            9,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localId = 77,
            localPath = "/local/live.jpg"
        )
        val livePhotoVideo = createTestOCFile(
            directory.fileId,
            "/live_video.mp4",
            10,
            ownerId = "user123",
            mimeType = "video/mp4",
            localPath = "/local/live_video.mp4"
        ).apply {
            setLivePhoto("77")
        }
        val offlineOCFile = createTestOCFile(
            directory.fileId,
            "/offline.jpg",
            11,
            ownerId = "user123",
            mimeType = MimeType.JPEG,
            localPath = "/local/offline.jpg"
        )

        val files =
            listOf(
                directory,
                hidden,
                image,
                video,
                temp,
                otherUsersFile,
                personal,
                shared,
                favorite,
                livePhotoImg,
                livePhotoVideo
            )
        dataProvider.setEntities(files)
        dataProvider.setOfflineFile(offlineOCFile)

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

    private fun createTestOCFile(
        parentId: Long,
        path: String,
        fileId: Long,
        ownerId: String? = null,
        mimeType: String? = MimeType.FILE,
        isHidden: Boolean = false,
        isFavorite: Boolean = false,
        isSharedViaLink: Boolean = false,
        localId: Long = -1,
        etag: String = "etag_$fileId",
        localPath: String? = null
    ): OCFile = OCFile(path).apply {
        this.parentId = parentId
        this.fileId = fileId
        this.remotePath = path
        this.ownerId = ownerId
        this.mimeType = mimeType
        this.isHidden = isHidden
        this.isFavorite = isFavorite
        this.isSharedViaLink = isSharedViaLink
        this.localId = localId
        this.etag = etag
        this.storagePath = localPath
    }
}
