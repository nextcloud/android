/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.resumption

import androidx.media3.common.MediaItem
import com.nextcloud.client.player.media3.common.MediaItemFactory
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.file.PlaybackFilesComparator
import com.nextcloud.client.player.model.file.PlaybackFilesRepository
import com.owncloud.android.ui.fragment.SearchType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test

class PlaybackResumptionLauncherTest {
    private val configStore = mockk<PlaybackResumptionConfigStore>()
    private val filesRepository = mockk<PlaybackFilesRepository>()
    private val mediaItemFactory = mockk<MediaItemFactory>()
    private val playbackModel = mockk<PlaybackModel>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var launcher: PlaybackResumptionLauncher

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { playbackModel.start() } just Runs

        launcher = PlaybackResumptionLauncher(
            playbackResumptionConfigStore = configStore,
            playbackFilesRepository = filesRepository,
            mediaItemFactory = mediaItemFactory,
            playbackModel = playbackModel
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun launch_success_uses_config_and_sets_files_flow() = runTest {
        val currentFileId = "2"
        val config = PlaybackResumptionConfig(
            currentFileId = currentFileId,
            folderId = 42L,
            fileType = PlaybackFileType.AUDIO,
            searchType = SearchType.FAVORITE_SEARCH
        )

        every { configStore.loadConfig() } returns config

        val file1 = PlaybackFile("1", "uri1", "n1", "audio/mpeg", 10, 0, false)
        val file2 = PlaybackFile("2", "uri2", "n2", "audio/mpeg", 11, 0, false)
        val file3 = PlaybackFile("3", "uri3", "n3", "audio/mpeg", 12, 0, false)

        val firstEmission = PlaybackFiles(listOf(file1, file2, file3), PlaybackFilesComparator.FAVORITE)
        val secondEmission = PlaybackFiles(listOf(file2, file3), PlaybackFilesComparator.FAVORITE)

        every {
            filesRepository.observe(config.folderId, config.fileType, config.searchType)
        } returns flow {
            emit(firstEmission)
            emit(secondEmission)
        }

        listOf(file1, file2, file3, file2, file3).forEach { file ->
            every { mediaItemFactory.create(file) } returns MediaItem.Builder()
                .setMediaId(file.id)
                .setUri(file.uri)
                .build()
        }

        val flowSlot = slot<Flow<PlaybackFiles>>()
        every { playbackModel.setFilesFlow(capture(flowSlot)) } just Runs

        val result = launcher.launch()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { playbackModel.start() }
        verify(exactly = 1) { playbackModel.setFilesFlow(any()) }
        assert(result.mediaItems.size == 3)
        assert(result.startIndex == 1)
        assert(result.mediaItems[1].mediaId == currentFileId)

        val collectedSecond = mutableListOf<PlaybackFiles>()
        val downstreamDispatcher = UnconfinedTestDispatcher(testScheduler)
        withContext(downstreamDispatcher) {
            flowSlot.captured.collect { collectedSecond += it }
        }

        assert(collectedSecond.size == 1)
        assert(collectedSecond.first().list.size == 2)
        assert(collectedSecond.first().list.first().id == currentFileId)
    }

    @Test
    fun launch_fallback_when_config_null_returns_stub() = runTest {
        every { configStore.loadConfig() } returns null
        every { mediaItemFactory.create(any()) } answers {
            val file = firstArg<PlaybackFile>()
            MediaItem.Builder().setMediaId(file.id).setUri(file.uri).build()
        }

        val result = launcher.launch()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { playbackModel.start() }
        assert(result.mediaItems.size == 1)
        assert(result.mediaItems.first().mediaId == "0")
        assert(result.startIndex == 0)
        verify(exactly = 0) { playbackModel.setFilesFlow(any()) }
    }
}
