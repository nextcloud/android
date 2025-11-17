/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.control

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.util.Optional

class PlayerControlViewTest {

    private lateinit var playbackModel: PlaybackModel
    private lateinit var view: PlayerControlView
    private lateinit var listenerSlot: CapturingSlot<PlaybackModel.Listener>

    @Before
    fun setup() {
        playbackModel = mockk(relaxed = true)
        listenerSlot = slot()

        every { playbackModel.addListener(capture(listenerSlot)) } returns Unit
        every { playbackModel.removeListener(any()) } returns Unit
        every { playbackModel.state } returns Optional.empty()

        val context = ApplicationProvider.getApplicationContext<Context>()
        view = PlayerControlView(context, injectedPlaybackModel = playbackModel)
        view.onStart()
    }

    @Test
    fun playPause_whenPlaying_invokesPause() {
        val files = listOf(mockFile())
        val itemState = itemState(PlayerState.PLAYING, 1_000, 5_000)
        pushState(playbackState(files = files, itemState = itemState))
        view.binding.ivPlayPause.performClick()
        verify { playbackModel.pause() }
    }

    @Test
    fun playPause_whenPaused_invokesPlay() {
        val files = listOf(mockFile())
        val itemState = itemState(PlayerState.PAUSED, 2_000, 6_000)
        pushState(playbackState(files = files, itemState = itemState))
        view.binding.ivPlayPause.performClick()
        verify { playbackModel.play() }
    }

    @Test
    fun repeat_clickFromAll_setsSingle() {
        pushState(playbackState(repeat = RepeatMode.ALL))
        view.binding.ivRepeat.performClick()
        verify { playbackModel.setRepeatMode(RepeatMode.SINGLE) }
    }

    @Test
    fun repeat_clickFromSingle_setsAll() {
        pushState(playbackState(repeat = RepeatMode.SINGLE))
        view.binding.ivRepeat.performClick()
        verify { playbackModel.setRepeatMode(RepeatMode.ALL) }
    }

    @Test
    fun shuffle_clickFromOff_enablesShuffle() {
        pushState(playbackState(shuffle = false))
        view.binding.ivRandom.performClick()
        verify { playbackModel.setShuffle(true) }
    }

    @Test
    fun shuffle_clickFromOn_disablesShuffle() {
        pushState(playbackState(shuffle = true))
        view.binding.ivRandom.performClick()
        verify { playbackModel.setShuffle(false) }
    }

    @Test
    fun nextPrevious_enablement_singleItem() {
        val files = listOf(mockFile())
        val itemState = itemState(PlayerState.PLAYING, 500, 2_000)
        pushState(playbackState(files = files, itemState = itemState))
        assert(!view.binding.ivNext.isEnabled)
        assert(view.binding.ivPrevious.isEnabled)
    }

    @Test
    fun nextPrevious_enablement_multipleItems() {
        val files = listOf(mockFile(), mockFile())
        val itemState = itemState(PlayerState.PAUSED, 500, 2_000)
        pushState(playbackState(files = files, itemState = itemState))
        assert(view.binding.ivNext.isEnabled)
        assert(view.binding.ivPrevious.isEnabled)
    }

    @Test
    fun nextPrevious_disabledWhenNoCurrentItem() {
        val files = listOf(mockFile(), mockFile())
        pushState(playbackState(files = files, itemState = null))
        assert(!view.binding.ivNext.isEnabled)
        assert(!view.binding.ivPrevious.isEnabled)
    }

    @Test
    fun progressBar_indeterminateWhenNoItem() {
        pushState(playbackState(itemState = null))
        assert(view.binding.progressBar.progress == 0)
        assert(view.binding.tvElapsed.text.toString() == "--:--")
        assert(view.binding.tvTotalTime.text.toString() == "--:--")
    }

    @Test
    fun progressBar_rendersValuesUnderHour() {
        val itemState = itemState(PlayerState.PLAYING, current = 65_000, max = 125_000)
        pushState(playbackState(itemState = itemState))
        assert(view.binding.progressBar.max == 125_000)
        assert(view.binding.progressBar.progress == 65_000)
        assert(view.binding.tvTotalTime.text.toString() == "02:05")
        assert(view.binding.tvElapsed.text.toString() == "01:05")
    }

    @Test
    fun progressBar_rendersValuesOverHour() {
        val itemState = itemState(PlayerState.PLAYING, current = 605_000, max = 3_726_000)
        pushState(playbackState(itemState = itemState))
        assert(view.binding.progressBar.max == 3_726_000)
        assert(view.binding.progressBar.progress == 605_000)
        assert(view.binding.tvTotalTime.text.toString() == "01:02:06")
        assert(view.binding.tvElapsed.text.toString() == "00:10:05")
    }

    private fun mockFile(): PlaybackFile = mockk(relaxed = true)

    private fun itemState(state: PlayerState, current: Long, max: Long) = PlaybackItemState(
        file = mockFile(),
        playerState = state,
        metadata = null,
        videoSize = null,
        currentTimeInMilliseconds = current,
        maxTimeInMilliseconds = max
    )

    private fun playbackState(
        files: List<PlaybackFile> = emptyList(),
        itemState: PlaybackItemState? = null,
        repeat: RepeatMode = RepeatMode.ALL,
        shuffle: Boolean = false
    ) = PlaybackState(
        currentFiles = files,
        currentItemState = itemState,
        repeatMode = repeat,
        shuffle = shuffle
    )

    private fun pushState(state: PlaybackState) {
        every { playbackModel.state } returns Optional.of(state)
        listenerSlot.captured.onPlaybackUpdate(state)
    }
}
