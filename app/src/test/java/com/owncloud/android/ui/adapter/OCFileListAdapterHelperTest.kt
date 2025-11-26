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

    private val userId = "user123"

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        mockkStatic(MainApp::class)
        every { MainApp.getAppContext() } returns context
        every { MainApp.isOnlyPersonFiles() } returns false
    }

    private inner class Sut {
        val root = directory("/", id = 0)

        fun directory(path: String, id: Long) = OCFile(path).apply {
            setFolder()
            fileId = id
            parentId = 0
            ownerId = userId
            remoteId = id.toString()
            remotePath = path
            mimeType = MimeType.DIRECTORY
            storagePath = ""
            etag = "etag_$id"
        }

        fun file(
            parent: OCFile,
            name: String,
            id: Long,
            mime: String = MimeType.FILE,
            hidden: Boolean = false,
            favorite: Boolean = false,
            shared: Boolean = false,
            localId: Long = -1,
            localPath: String = ""
        ) = OCFile("/$name").apply {
            parentId = parent.fileId
            fileId = id
            remotePath = "/$name"
            ownerId = userId
            mimeType = mime
            isHidden = hidden
            isFavorite = favorite
            isSharedViaLink = shared
            this.localId = localId
            etag = "etag_$id"
            storagePath = localPath
        }

        fun prepare(files: List<OCFile>, offline: OCFile? = null) {
            dataProvider.setEntities(files)
            offline?.let { dataProvider.setOfflineFile(it) }
        }

        suspend fun run(directory: OCFile, mime: String = "") = helper.prepareFileList(
            directory = directory,
            dataProvider = dataProvider,
            onlyOnDevice = false,
            limitToMimeType = mime,
            preferences = preferences,
            userId = userId
        )
    }

    private fun stubPreferences(
        showHidden: Boolean = false,
        sort: FileSortOrder,
        folderFirst: Boolean = true,
        favFirst: Boolean = false
    ) {
        every { preferences.isShowHiddenFilesEnabled() } returns showHidden
        every { preferences.getSortOrderByFolder(any()) } returns sort
        every { preferences.isSortFoldersBeforeFiles() } returns folderFirst
        every { preferences.isSortFavoritesFirst() } returns favFirst
    }

    @Test
    fun `prepareFileList with multiple folders and sort Z to A`() = runBlocking {
        val env = Sut()
        val root = env.root

        val sub1 = env.directory("/subDir", 1)
        val sub2 = env.directory("/subDir2", 2)

        val fImage = env.file(root, "image.jpg", 11, MimeType.JPEG)
        val fVideo = env.file(root, "video.mp4", 12, MimeType.MP4)
        val fSub = env.file(sub1, "video2.mp4", 21, MimeType.MP4)

        env.prepare(listOf(root, sub1, sub2, fImage, fVideo, fSub))

        stubPreferences(sort = FileSortOrder.SORT_Z_TO_A)

        val (list, sort) = env.run(root)

        assertEquals(listOf("subDir2", "subDir", "video.mp4", "image.jpg"), list.map { it.fileName })
        assertEquals(FileSortOrder.SORT_Z_TO_A, sort)
    }

    @Test
    fun `prepareFileList with multiple folders and favorites first`() = runBlocking {
        val env = Sut()
        val root = env.root

        val sub1 = env.directory("/subDir", 1)
        val sub2 = env.directory("/subDir2", 2)

        val fImage = env.file(root, "image.jpg", 11, MimeType.JPEG)
        val fVideo = env.file(root, "video.mp4", 12, MimeType.MP4)
        val fFav = env.file(root, "fav_image.jpg", 19, MimeType.JPEG, favorite = true)
        val fSub = env.file(sub1, "video2.mp4", 21, MimeType.MP4)

        env.prepare(listOf(root, sub1, sub2, fImage, fVideo, fFav, fSub))

        stubPreferences(sort = FileSortOrder.SORT_A_TO_Z, favFirst = true)

        val (list, sort) = env.run(root)

        assertEquals(
            listOf("fav_image.jpg", "subDir", "subDir2", "image.jpg", "video.mp4"),
            list.map { it.fileName }
        )
        assertEquals(FileSortOrder.SORT_A_TO_Z, sort)
    }

    @Test
    fun `prepareFileList with multiple folders`() = runBlocking {
        val env = Sut()
        val root = env.root

        val sub1 = env.directory("/subDir", 1)
        val sub2 = env.directory("/subDir2", 2)

        val fImg = env.file(root, "image.jpg", 11, MimeType.JPEG)
        val fVid = env.file(root, "video.mp4", 12, MimeType.MP4)
        val fSubVid = env.file(sub1, "video2.mp4", 21, MimeType.MP4)

        env.prepare(listOf(root, sub1, sub2, fImg, fVid, fSubVid))

        stubPreferences(sort = FileSortOrder.SORT_A_TO_Z)

        val (list, sort) = env.run(root)

        assertEquals(listOf("subDir", "subDir2", "image.jpg", "video.mp4"), list.map { it.fileName })
        assertEquals(FileSortOrder.SORT_A_TO_Z, sort)
    }

    @Test
    fun `prepareFileList hides hidden files and sorts A to Z`() = runBlocking {
        val env = Sut()
        val root = env.root

        val fHidden = env.file(root, ".hidden.jpg", 1, MimeType.JPEG, hidden = true)
        val fImg = env.file(root, "image.jpg", 2, MimeType.JPEG)
        val fVid = env.file(root, "video.mp4", 3, MimeType.MP4)
        val fTemp = env.file(root, "temp.tmp", 4, MimeType.FILE)
        val fOther = env.file(env.directory("/other", 202), "other.jpg", 5, MimeType.JPEG)
        val fPersonal = env.file(root, "personal.jpg", 6, MimeType.JPEG)
        val fShared = env.file(root, "shared.jpg", 7, MimeType.JPEG, shared = true)
        val fFav = env.file(root, "favorite.jpg", 8, MimeType.JPEG, favorite = true)
        val fLiveImg = env.file(root, "live.jpg", 9, MimeType.JPEG, localId = 77)
        val fLiveVid = env.file(root, "live_video.mp4", 10, MimeType.MP4).apply { setLivePhoto("77") }
        val offline = env.file(root, "offline.jpg", 11, MimeType.JPEG)

        env.prepare(
            listOf(
                root, fHidden, fImg, fVid, fTemp, fOther, fPersonal,
                fShared, fFav, fLiveImg, fLiveVid
            ),
            offline = offline
        )

        stubPreferences(sort = FileSortOrder.SORT_A_TO_Z)

        val (list, sort) = env.run(root, mime = "image")

        assertEquals(
            listOf("favorite.jpg", "image.jpg", "live.jpg", "offline.jpg", "personal.jpg", "shared.jpg"),
            list.map { it.fileName }
        )
        assertEquals(FileSortOrder.SORT_A_TO_Z, sort)
    }
}
