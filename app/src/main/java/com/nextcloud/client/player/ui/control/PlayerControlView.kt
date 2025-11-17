/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.control

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import com.nextcloud.client.player.util.setTint
import com.owncloud.android.R
import com.owncloud.android.databinding.PlayerControlViewBinding
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

private const val INDETERMINATE_TIME = "--:--"
private const val TAG_CLICK_COMMAND_PLAY = "TAG_CLICK_COMMAND_PLAY"
private const val TAG_CLICK_COMMAND_PAUSE = "TAG_CLICK_COMMAND_PAUSE"
private const val TAG_CLICK_COMMAND_REPEAT = "TAG_CLICK_COMMAND_REPEAT"
private const val TAG_CLICK_COMMAND_DO_NOT_REPEAT = "TAG_CLICK_COMMAND_DO_NOT_REPEAT"
private const val TAG_CLICK_COMMAND_SHUFFLE = "TAG_CLICK_COMMAND_SHUFFLE"
private const val TAG_CLICK_COMMAND_DO_NOT_SHUFFLE = "TAG_CLICK_COMMAND_DO_NOT_SHUFFLE"
private const val TAG_CLICK_COMMAND_UNKNOWN = "TAG_CLICK_COMMAND_UNKNOWN"

private const val PROGRESS_CHANGE_DEBOUNCE_MS = 200L
private const val DEFAULT_MIN_PROGRESS = 0
private const val DEFAULT_MAX_PROGRESS = 100
private const val MILLISECONDS_IN_SECOND = 1000
private const val MILLISECONDS_IN_HOUR = 3_600_000
private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60

class PlayerControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    injectedPlaybackModel: PlaybackModel? = null
) : LinearLayout(context, attrs, defStyleAttr),
    PlaybackModel.Listener {

    @Inject
    lateinit var playbackModel: PlaybackModel

    private val seekBarProgressChangeFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private var viewScope: CoroutineScope? = null

    val binding = PlayerControlViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        if (!isInEditMode) {
            if (injectedPlaybackModel == null) {
                (context.applicationContext as HasAndroidInjector).androidInjector().inject(this)
            } else {
                playbackModel = injectedPlaybackModel
            }
            setDefaultTags()
            setListeners()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            collectSeekBarChanges()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            viewScope?.cancel()
            viewScope = null
        }
        super.onDetachedFromWindow()
    }

    fun onStart() {
        playbackModel.state.ifPresent(::render)
        playbackModel.addListener(this)
    }

    fun onStop() {
        playbackModel.removeListener(this)
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    private fun setDefaultTags() {
        binding.ivPlayPause.tag = TAG_CLICK_COMMAND_UNKNOWN
        binding.ivRandom.tag = TAG_CLICK_COMMAND_UNKNOWN
        binding.ivRepeat.tag = TAG_CLICK_COMMAND_UNKNOWN
    }

    private fun setListeners() {
        binding.ivPlayPause.setOnClickListener {
            when (binding.ivPlayPause.tag) {
                TAG_CLICK_COMMAND_PLAY -> playbackModel.play()
                TAG_CLICK_COMMAND_PAUSE -> playbackModel.pause()
            }
        }

        binding.ivRepeat.setOnClickListener {
            when (binding.ivRepeat.tag) {
                TAG_CLICK_COMMAND_REPEAT -> playbackModel.setRepeatMode(RepeatMode.SINGLE)
                TAG_CLICK_COMMAND_DO_NOT_REPEAT -> playbackModel.setRepeatMode(RepeatMode.ALL)
            }
        }

        binding.ivRandom.setOnClickListener {
            playbackModel.setShuffle(binding.ivRandom.tag == TAG_CLICK_COMMAND_SHUFFLE)
        }

        binding.ivNext.setOnClickListener { playbackModel.playNext() }

        binding.ivPrevious.setOnClickListener(object : MultipleClickListener() {
            override fun onSingleClick(view: View?) {
                val state = playbackModel.state.getOrNull()?.currentItemState ?: return
                if (state.playerState == PlayerState.PAUSED || state.playerState == PlayerState.PLAYING) {
                    playbackModel.seekToPosition(0L)
                } else {
                    playbackModel.playPrevious()
                }
            }

            override fun onDoubleClick(view: View?) {
                playbackModel.playPrevious()
            }
        })

        binding.progressBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seekBarProgressChangeFlow.tryEmit(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    @OptIn(FlowPreview::class)
    private fun collectSeekBarChanges() {
        val viewScope = viewScope ?: return
        val lifecycleOwner = (context as? LifecycleOwner) ?: return
        seekBarProgressChangeFlow
            .debounce(PROGRESS_CHANGE_DEBOUNCE_MS)
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .onEach { playbackModel.seekToPosition(it.toLong()) }
            .launchIn(viewScope)
    }

    private fun render(playbackState: PlaybackState) {
        renderRepeatButton(playbackState.repeatMode == RepeatMode.SINGLE)
        renderShuffleButton(playbackState.shuffle)
        renderPlayPauseButton(playbackState.currentItemState?.playerState == PlayerState.PLAYING)
        renderNextPreviousButtons(playbackState)
        renderProgressBar(playbackState.currentItemState)
    }

    private fun renderRepeatButton(repeatSingle: Boolean) {
        binding.ivRepeat.setTint(if (repeatSingle) R.color.player_accent_color else R.color.player_default_icon_color)
        binding.ivRepeat.tag = if (repeatSingle) TAG_CLICK_COMMAND_DO_NOT_REPEAT else TAG_CLICK_COMMAND_REPEAT
    }

    private fun renderShuffleButton(shuffle: Boolean) {
        binding.ivRandom.setTint(if (shuffle) R.color.player_accent_color else R.color.player_default_icon_color)
        binding.ivRandom.tag = if (shuffle) TAG_CLICK_COMMAND_DO_NOT_SHUFFLE else TAG_CLICK_COMMAND_SHUFFLE
    }

    private fun renderPlayPauseButton(isPlaying: Boolean) {
        binding.ivPlayPause.setImageResource(if (isPlaying) R.drawable.player_ic_pause else R.drawable.player_ic_play)
        binding.ivPlayPause.tag = if (isPlaying) TAG_CLICK_COMMAND_PAUSE else TAG_CLICK_COMMAND_PLAY
    }

    private fun renderNextPreviousButtons(playbackState: PlaybackState) {
        binding.ivNext.setEnabled(playbackState.currentItemState != null && playbackState.currentFiles.size > 1)
        binding.ivPrevious.setEnabled(playbackState.currentItemState != null && playbackState.currentFiles.isNotEmpty())
    }

    private fun renderProgressBar(playbackItemState: PlaybackItemState?) {
        val enabled = playbackItemState != null && playbackItemState.maxTimeInMilliseconds > DEFAULT_MIN_PROGRESS
        val max = if (enabled) playbackItemState.maxTimeInMilliseconds.toInt() else DEFAULT_MAX_PROGRESS
        val progress = if (enabled) playbackItemState.currentTimeInMilliseconds.toInt() else DEFAULT_MIN_PROGRESS
        binding.progressBar.isEnabled = enabled
        binding.progressBar.max = max
        binding.progressBar.progress = progress
        binding.tvElapsed.text = if (enabled) formatTime(progress, max) else INDETERMINATE_TIME
        binding.tvTotalTime.text = if (enabled) formatTime(max, max) else INDETERMINATE_TIME
    }

    private fun formatTime(current: Int, max: Int): String {
        val seconds = current / MILLISECONDS_IN_SECOND
        val minutes = seconds / SECONDS_IN_MINUTE
        val hours = minutes / MINUTES_IN_HOUR
        return if (max >= MILLISECONDS_IN_HOUR) {
            "%02d:%02d:%02d".format(hours, minutes % MINUTES_IN_HOUR, seconds % SECONDS_IN_MINUTE)
        } else {
            "%02d:%02d".format(minutes, seconds % SECONDS_IN_MINUTE)
        }
    }
}
