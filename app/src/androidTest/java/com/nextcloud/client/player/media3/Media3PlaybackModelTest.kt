/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3

import android.content.Context
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.nextcloud.client.player.media3.common.MediaItemFactory
import com.nextcloud.client.player.media3.common.TestPlayerFactory
import com.nextcloud.client.player.media3.controller.TestMediaControllerFactory
import com.nextcloud.client.player.media3.session.MediaSessionHolder
import com.nextcloud.client.player.media3.session.TestMediaSessionFactory
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.PlaybackSettings
import com.nextcloud.client.player.model.error.DefaultPlaybackErrorStrategy
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.file.PlaybackFilesComparator
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Media3PlaybackModelTest {
    private lateinit var settings: PlaybackSettings
    private lateinit var model: PlaybackModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    @UiThreadTest
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val playerFactory = TestPlayerFactory()
        val sessionFactory = TestMediaSessionFactory(context, playerFactory)

        settings = PlaybackSettings(context)

        model = Media3PlaybackModel(
            PlaybackStateFactory(),
            sessionFactory,
            TestMediaControllerFactory(context) { model as MediaSessionHolder },
            settings,
            MediaItemFactory(),
            DefaultPlaybackErrorStrategy()
        )

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @UiThreadTest
    fun start_initialState() = runModelTest { model ->
        model.state.get().let { state ->
            assertTrue(state.currentFiles.isEmpty())
            assertNull(state.currentItemState)
            assertEquals(settings.repeatMode, state.repeatMode)
            assertEquals(settings.isShuffle, state.shuffle)
        }
    }

    @Test
    @UiThreadTest
    fun setFiles_initialQueue() = runModelTest { model ->
        val inputFiles = playbackFiles(3)

        model.setFiles(inputFiles)

        model.state.get().let { state ->
            assertEquals(3, state.currentFiles.size)
            assertNotEquals(PlayerState.PLAYING, state.currentItemState!!.playerState)
        }

        model.play()

        model.state.get().let { state ->
            assertEquals(PlayerState.PLAYING, state.currentItemState!!.playerState)
            assertEquals(inputFiles.list.first().id, state.currentItemState.file.id)
        }
    }

    @Test
    @UiThreadTest
    fun setFiles_updateRetainCurrentItem() = runModelTest { model ->
        val inputFiles = playbackFiles(3)
        val initialFileId = inputFiles.list.first().id

        model.setFiles(inputFiles)
        model.play()

        model.state.get().let { state ->
            assertEquals(3, state.currentFiles.size)
            assertEquals(0, state.currentFiles.indexOf(state.currentItemState!!.file))
            assertEquals(initialFileId, state.currentItemState.file.id)
        }

        model.setFiles(inputFiles.copy(list = listOf(file(99)) + inputFiles.list))

        model.state.get().let { state ->
            assertEquals(4, state.currentFiles.size)
            assertEquals(1, state.currentFiles.indexOf(state.currentItemState!!.file))
            assertEquals(initialFileId, state.currentItemState.file.id)
        }
    }

    @Test
    @UiThreadTest
    fun setFiles_repositionWhenCurrentRemoved() = runModelTest { model ->
        val inputFiles = playbackFiles(3)
        val initialFileId = inputFiles.list.first().id

        model.setFiles(inputFiles)
        model.play()

        model.state.get().let { state ->
            assertEquals(3, state.currentFiles.size)
            assertEquals(0, state.currentFiles.indexOf(state.currentItemState!!.file))
            assertEquals(initialFileId, state.currentItemState.file.id)
        }

        model.setFiles(inputFiles.copy(list = inputFiles.list.filter { it.id != initialFileId }))

        model.state.get().let { state ->
            assertEquals(2, state.currentFiles.size)
            assertEquals(0, state.currentFiles.indexOf(state.currentItemState!!.file))
            assertNotEquals(initialFileId, state.currentItemState.file.id)
        }
    }

    @Test
    @UiThreadTest
    fun playback_playPauseStop() = runModelTest { model ->
        model.setFiles(playbackFiles(2))

        model.play()
        assertEquals(PlayerState.PLAYING, model.state.get().currentItemState!!.playerState)

        model.pause()
        assertEquals(PlayerState.PAUSED, model.state.get().currentItemState!!.playerState)
    }

    @Test
    @UiThreadTest
    fun playback_nextPrevious() = runModelTest { model ->
        model.setFiles(playbackFiles(3))

        model.play()
        model.state.get().let { state ->
            assertEquals(0, state.currentFiles.indexOf(state.currentItemState!!.file))
        }

        model.playNext()
        model.state.get().let { state ->
            assertEquals(1, state.currentFiles.indexOf(state.currentItemState!!.file))
        }

        model.playPrevious()
        model.state.get().let { state ->
            assertEquals(0, state.currentFiles.indexOf(state.currentItemState!!.file))
        }
    }

    @Test
    @UiThreadTest
    fun playback_seekToPosition() = runModelTest { model ->
        model.setFiles(playbackFiles(1))
        model.seekToPosition(5000L)
        model.play()
        assertEquals(5000L, model.state.get().currentItemState!!.currentTimeInMilliseconds)
    }

    @Test
    @UiThreadTest
    fun settings_repeatAndShuffle() = runModelTest { model ->
        model.setFiles(playbackFiles(2))
        model.setRepeatMode(RepeatMode.ALL)
        model.setShuffle(true)

        model.state.get().let { state ->
            assertEquals(RepeatMode.ALL, state.repeatMode)
            assertTrue(state.shuffle)
        }

        model.setRepeatMode(RepeatMode.SINGLE)
        model.setShuffle(false)

        model.state.get().let { state ->
            assertEquals(RepeatMode.SINGLE, state.repeatMode)
            assertFalse(state.shuffle)
        }
    }

    @Test
    @UiThreadTest
    fun switchToFile_changesCurrentItem() = runModelTest { model ->
        val inputFiles = playbackFiles(4)

        model.setFiles(inputFiles)
        model.play()

        model.state.get().let { state ->
            assertEquals(0, state.currentFiles.indexOf(state.currentItemState!!.file))
            assertEquals(inputFiles.list.first().id, state.currentItemState.file.id)
        }

        model.switchToFile(inputFiles.list.last())

        model.state.get().let { state ->
            assertEquals(3, state.currentFiles.indexOf(state.currentItemState!!.file))
            assertEquals(inputFiles.list.last().id, state.currentItemState.file.id)
        }
    }

    @Test
    @UiThreadTest
    fun setFilesFlow_emitsAndUpdates() = runModelTest { model ->
        val filesFlow = MutableStateFlow(playbackFiles(2))
        model.setFilesFlow(filesFlow)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, model.state.get().currentFiles.size)

        filesFlow.tryEmit(playbackFiles(3))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, model.state.get().currentFiles.size)
    }

    private fun runModelTest(test: suspend TestScope.(PlaybackModel) -> Unit) = runTest {
        model.start()
        test(model)
        model.release()
        settings.reset()
    }

    private fun file(id: Int) = PlaybackFile(
        id = "id$id",
        uri = "https://example.com/media$id.mp3",
        name = "media$id.mp3",
        mimeType = "audio/mpeg",
        contentLength = 123456,
        lastModified = System.currentTimeMillis() + id,
        isFavorite = false
    )

    private fun playbackFiles(count: Int): PlaybackFiles {
        val list = (0 until count).map { file(it) }
        return PlaybackFiles(list, PlaybackFilesComparator.NONE)
    }
}
