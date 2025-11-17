/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import android.content.ContentUris
import android.net.Uri
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlaybackFilesRepositoryTest {
    private lateinit var storageManager: FileDataStorageManager
    private lateinit var preferences: AppPreferences
    private lateinit var repository: PlaybackFilesRepository
    private lateinit var contentObserver: FakeContentObserver

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val favorites = listOf(
        ocFile("/files/user/d1.docx", favorite = true),
        ocFile("/files/user/a2.mp3", favorite = true),
        ocFile("/files/user/v2.mkv", favorite = true),
        ocFile("/files/user/i1.jpg", favorite = true),
        ocFile("/files/user/a1.flac", favorite = true),
        ocFile("/files/user/v1.mp4", favorite = true)
    )

    private val galleryItems = listOf(
        ocFile("/files/user/i1.jpg", lastModified = 100L),
        ocFile("/files/user/v1.mp4", lastModified = 200L),
        ocFile("/files/user/i2.png", lastModified = 300L),
        ocFile("/files/user/v2.mkv", lastModified = 400L)
    )

    private val shares = listOf(
        ocShare("/files/user/d1.docx", shareDate = 100L),
        ocShare("/files/user/a1.mp3", shareDate = 200L),
        ocShare("/files/user/v1.mkv", shareDate = 300L),
        ocShare("/files/user/i1.jpg", shareDate = 400L),
        ocShare("/files/user/a2.flac", shareDate = 500L),
        ocShare("/files/user/v2.mp4", shareDate = 600L)
    )

    private val folderItems = listOf(
        ocFile("/files/user/folder/d1.docx", favorite = false),
        ocFile("/files/user/folder/a1.mp3", favorite = false),
        ocFile("/files/user/folder/v1.mkv", favorite = false),
        ocFile("/files/user/folder/i1.jpg", favorite = false),
        ocFile("/files/user/folder/a2.flac", favorite = true),
        ocFile("/files/user/folder/v2.mp4", favorite = true)
    )

    private val folder = OCFile("/files/user/folder").apply {
        localId = 1234L
        mimeType = "DIR"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        storageManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        contentObserver = FakeContentObserver()
        repository = PlaybackFilesRepository(
            storageManager,
            preferences,
            testDispatcher,
            contentObserver
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun get_favorite_audio_playback_files() = testScope.runTest {
        every { storageManager.favoriteFiles } returns favorites
        val playbackFiles = repository.get(0L, PlaybackFileType.AUDIO, SearchType.FAVORITE_SEARCH)
        assertEquals(listOf("a1.flac", "a2.mp3"), playbackFiles.list.map { it.name })
    }

    @Test
    fun get_favorite_video_playback_files() = testScope.runTest {
        every { storageManager.favoriteFiles } returns favorites
        val playbackFiles = repository.get(0L, PlaybackFileType.VIDEO, SearchType.FAVORITE_SEARCH)
        assertEquals(listOf("v1.mp4", "v2.mkv"), playbackFiles.list.map { it.name })
    }

    @Test
    fun observe_favorite_audio_playback_files() {
        val favorites = favorites.toMutableList()
        every { storageManager.favoriteFiles } answers { favorites }
        assertObserve(
            flow = repository.observe(0L, PlaybackFileType.AUDIO, SearchType.FAVORITE_SEARCH),
            contentUri = ProviderTableMeta.CONTENT_URI,
            trigger1 = { favorites.add(ocFile("/files/user/a4.mp3")) },
            trigger2 = { favorites.add(ocFile("/files/user/a3.mp3")) },
            expected1 = listOf("a1.flac", "a2.mp3"),
            expected2 = listOf("a1.flac", "a2.mp3", "a3.mp3", "a4.mp3")
        )
    }

    @Test
    fun observe_favorite_video_playback_files() {
        val favorites = favorites.toMutableList()
        every { storageManager.favoriteFiles } answers { favorites }
        assertObserve(
            flow = repository.observe(0L, PlaybackFileType.VIDEO, SearchType.FAVORITE_SEARCH),
            contentUri = ProviderTableMeta.CONTENT_URI,
            trigger1 = { favorites.add(ocFile("/files/user/v4.mp4")) },
            trigger2 = { favorites.add(ocFile("/files/user/v3.mp4")) },
            expected1 = listOf("v1.mp4", "v2.mkv"),
            expected2 = listOf("v1.mp4", "v2.mkv", "v3.mp4", "v4.mp4")
        )
    }

    @Test
    fun get_gallery_video_playback_files() = testScope.runTest {
        every { storageManager.allGalleryItems } returns galleryItems
        val playbackFiles = repository.get(0L, PlaybackFileType.VIDEO, SearchType.GALLERY_SEARCH)
        assertEquals(listOf("v2.mkv", "v1.mp4"), playbackFiles.list.map { it.name })
    }

    @Test
    fun observe_gallery_video_playback_files() {
        val galleryItems = galleryItems.toMutableList()
        every { storageManager.allGalleryItems } answers { galleryItems }
        assertObserve(
            flow = repository.observe(0L, PlaybackFileType.VIDEO, SearchType.GALLERY_SEARCH),
            contentUri = ProviderTableMeta.CONTENT_URI,
            trigger1 = { galleryItems.add(ocFile("/files/user/v3.mp4", lastModified = 500L)) },
            trigger2 = { galleryItems.add(ocFile("/files/user/v4.mp4", lastModified = 600L)) },
            expected1 = listOf("v2.mkv", "v1.mp4"),
            expected2 = listOf("v4.mp4", "v3.mp4", "v2.mkv", "v1.mp4")
        )
    }

    @Test
    fun get_shared_audio_playback_files() = testScope.runTest {
        every { storageManager.shares } returns shares
        val playbackFiles = repository.get(0L, PlaybackFileType.AUDIO, SearchType.SHARED_FILTER)
        assertEquals(listOf("a2.flac", "a1.mp3"), playbackFiles.list.map { it.name })
    }

    @Test
    fun get_shared_video_playback_files() = testScope.runTest {
        every { storageManager.shares } returns shares
        val playbackFiles = repository.get(0L, PlaybackFileType.VIDEO, SearchType.SHARED_FILTER)
        assertEquals(listOf("v2.mp4", "v1.mkv"), playbackFiles.list.map { it.name })
    }

    @Test
    fun observe_shared_audio_playback_files() {
        val shares = shares.toMutableList()
        every { storageManager.shares } answers { shares }
        assertObserve(
            flow = repository.observe(0L, PlaybackFileType.AUDIO, SearchType.SHARED_FILTER),
            contentUri = ProviderTableMeta.CONTENT_URI_SHARE,
            trigger1 = { shares.add(ocShare("/files/user/a3.mp3", 700L)) },
            trigger2 = { shares.add(ocShare("/files/user/a4.mp3", 800L)) },
            expected1 = listOf("a2.flac", "a1.mp3"),
            expected2 = listOf("a4.mp3", "a3.mp3", "a2.flac", "a1.mp3")
        )
    }

    @Test
    fun observe_shared_video_playback_files() {
        val shares = shares.toMutableList()
        every { storageManager.shares } answers { shares }
        assertObserve(
            flow = repository.observe(0L, PlaybackFileType.VIDEO, SearchType.SHARED_FILTER),
            contentUri = ProviderTableMeta.CONTENT_URI_SHARE,
            trigger1 = { shares.add(ocShare("/files/user/v3.mp4", 700L)) },
            trigger2 = { shares.add(ocShare("/files/user/v4.mp4", 800L)) },
            expected1 = listOf("v2.mp4", "v1.mkv"),
            expected2 = listOf("v4.mp4", "v3.mp4", "v2.mp4", "v1.mkv")
        )
    }

    @Test
    fun get_folder_audio_playback_files() = testScope.runTest {
        mockFolder(folder, folderItems)
        val playbackFiles = repository.get(folder.localId, PlaybackFileType.AUDIO, null)
        assertEquals(listOf("a2.flac", "a1.mp3"), playbackFiles.list.map { it.name })
    }

    @Test
    fun get_folder_video_playback_files() = testScope.runTest {
        mockFolder(folder, folderItems)
        val playbackFiles = repository.get(folder.localId, PlaybackFileType.VIDEO, null)
        assertEquals(listOf("v2.mp4", "v1.mkv"), playbackFiles.list.map { it.name })
    }

    @Test
    fun observe_folder_audio_playback_files() {
        val folderItems = folderItems.toMutableList()
        mockFolder(folder, folderItems)
        assertObserve(
            flow = repository.observe(folder.localId, PlaybackFileType.AUDIO, null),
            contentUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, folder.localId),
            trigger1 = { folderItems.add(ocFile("/files/user/folder/a3.mp3", favorite = true)) },
            trigger2 = { folderItems.add(ocFile("/files/user/folder/a4.mp3", favorite = false)) },
            expected1 = listOf("a2.flac", "a1.mp3"),
            expected2 = listOf("a2.flac", "a3.mp3", "a1.mp3", "a4.mp3")
        )
    }

    @Test
    fun observe_folder_video_playback_files() {
        val folderItems = folderItems.toMutableList()
        mockFolder(folder, folderItems)
        assertObserve(
            flow = repository.observe(folder.localId, PlaybackFileType.VIDEO, null),
            contentUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, folder.localId),
            trigger1 = { folderItems.add(ocFile("/files/user/folder/v3.mp4", favorite = false)) },
            trigger2 = { folderItems.add(ocFile("/files/user/folder/v4.mp4", favorite = true)) },
            expected1 = listOf("v2.mp4", "v1.mkv"),
            expected2 = listOf("v2.mp4", "v4.mp4", "v1.mkv", "v3.mp4")
        )
    }

    private fun assertObserve(
        flow: Flow<PlaybackFiles>,
        contentUri: Uri,
        trigger1: () -> Unit,
        trigger2: () -> Unit,
        expected1: List<String>,
        expected2: List<String>
    ) = testScope.runTest {
        val emissions = mutableListOf<PlaybackFiles>()
        val job = launch { flow.toList(emissions) }

        advanceUntilIdle()
        assertEquals(expected1, emissions[0].list.map { it.name })

        trigger1()
        contentObserver.emit(contentUri)
        delay(100L)
        trigger2()
        contentObserver.emit(contentUri)

        advanceUntilIdle()
        assertEquals(2, emissions.size)
        assertEquals(expected2, emissions[1].list.map { it.name })

        job.cancel()
    }

    private fun mockFolder(folder: OCFile, items: List<OCFile>) {
        every { storageManager.getFileById(folder.localId) } returns folder
        every { storageManager.getFolderContent(folder, any()) } returns items
        every { preferences.getSortOrderByFolder(folder) } returns FileSortOrder.SORT_A_TO_Z
    }

    private fun ocFile(path: String, lastModified: Long = 0L, favorite: Boolean = false): OCFile = OCFile(path).apply {
        localId = path.hashCode().toLong()
        mimeType = MimeTypeUtil.getMimeTypeFromPath(path)
        modificationTimestamp = lastModified
        isFavorite = favorite
    }

    private fun ocShare(path: String, shareDate: Long): OCShare = OCShare(path).apply {
        fileSource = path.hashCode().toLong()
        mimetype = MimeTypeUtil.getMimeTypeFromPath(path)
        sharedDate = shareDate
    }

    class FakeContentObserver : (Uri, Boolean) -> Flow<Boolean> {
        private val map = mutableMapOf<Uri, MutableSharedFlow<Boolean>>()

        override fun invoke(uri: Uri, notify: Boolean): Flow<Boolean> = map.getOrPut(uri) {
            MutableSharedFlow(extraBufferCapacity = 16)
        }

        fun emit(uri: Uri) {
            map[uri]?.tryEmit(true)
        }
    }
}
