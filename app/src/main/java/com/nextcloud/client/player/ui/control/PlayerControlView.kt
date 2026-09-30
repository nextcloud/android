/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.control

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import com.nextcloud.client.player.ui.MediaNavigator
import com.owncloud.android.R
import com.owncloud.android.databinding.PlayerControlViewBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
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
import kotlin.time.Duration.Companion.milliseconds

private const val INDETERMINATE_TIME = "--:--"

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
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr),
    PlaybackModel.Listener {

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var colorUtil: ColorUtil

    private val seekPositionFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private var viewScope: CoroutineScope? = null

    val binding = PlayerControlViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val serverPrimaryColor: Int by lazy { viewThemeUtils.getScheme(context).sourceColorArgb }

    private val serverPrimaryTint: ColorStateList by lazy { ColorStateList.valueOf(serverPrimaryColor) }

    private val onServerPrimaryTint: ColorStateList by lazy {
        ColorStateList.valueOf(colorUtil.getForegroundColorForBackgroundColor(serverPrimaryColor))
    }

    private val transportIconTint: ColorStateList? by lazy {
        ContextCompat.getColorStateList(context, R.color.player_control_icon_tint)
    }

    private val toggleIconTint: ColorStateList by lazy {
        ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(serverPrimaryColor, ContextCompat.getColor(context, R.color.player_default_icon_color))
        )
    }

    var navigator: MediaNavigator? = null

    init {
        if (!isInEditMode) {
            (context.applicationContext as HasAndroidInjector).androidInjector().inject(this)
            themeControls()
            setListeners()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            collectSeekPositions()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            viewScope?.cancel()
            viewScope = null
        }
        navigator = null
        super.onDetachedFromWindow()
    }

    fun onStart() {
        playbackModel.state?.let(::render)
        playbackModel.addListener(this)
    }

    fun onStop() {
        playbackModel.removeListener(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Clear insets to avoid covering ui
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = rootWindowInsets.getInsets(WindowInsets.Type.systemBars())
            binding.playerControlPanel.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    private fun themeControls() {
        binding.run {
            ivPrevious.iconTint = transportIconTint
            ivNext.iconTint = transportIconTint
            ivRepeat.iconTint = toggleIconTint
            ivRandom.iconTint = toggleIconTint
            ivPlayPause.backgroundTintList = serverPrimaryTint
            ivPlayPause.iconTint = onServerPrimaryTint

            slider.trackActiveTintList = serverPrimaryTint
            slider.thumbTintList = serverPrimaryTint
            slider.setLabelFormatter { formatTime(it.toInt(), slider.valueTo.toInt()) }
        }
    }

    private fun setListeners() {
        binding.ivPlayPause.setOnClickListener {
            if (binding.ivPlayPause.isChecked) {
                playbackModel.play()
            } else {
                playbackModel.pause()
            }
        }

        binding.ivRepeat.setOnClickListener {
            val repeatSingle = binding.ivRepeat.isChecked

            // Repeat and Random are mutually exclusive
            playbackModel.setShuffle(false)

            playbackModel.setRepeatMode(if (repeatSingle) RepeatMode.SINGLE else RepeatMode.ALL)
        }

        binding.ivRandom.setOnClickListener {
            val shuffle = binding.ivRandom.isChecked

            // Repeat and Random are mutually exclusive
            playbackModel.setRepeatMode(RepeatMode.OFF)

            playbackModel.setShuffle(shuffle)
        }

        binding.ivNext.setOnClickListener { navigator?.showNext() }

        binding.ivPrevious.setOnClickListener { navigator?.showPrevious() }

        binding.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                seekPositionFlow.tryEmit(value.toInt())
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun collectSeekPositions() {
        val viewScope = viewScope ?: return
        val lifecycleOwner = (context as? LifecycleOwner) ?: return
        seekPositionFlow
            .debounce(PROGRESS_CHANGE_DEBOUNCE_MS.milliseconds)
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .onEach { playbackModel.seekToPosition(it.toLong()) }
            .launchIn(viewScope)
    }

    private fun render(playbackState: PlaybackState) {
        binding.ivRepeat.isChecked = playbackState.repeatMode == RepeatMode.SINGLE
        binding.ivRandom.isChecked = playbackState.shuffle
        binding.ivPlayPause.isChecked = playbackState.currentItemState?.playerState == PlayerState.PLAYING
        binding.ivNext.isEnabled = navigator?.hasNext == true
        binding.ivPrevious.isEnabled = navigator?.hasPrevious == true
        renderProgress(playbackState.currentItemState)
    }

    private fun renderProgress(playbackItemState: PlaybackItemState?) {
        val enabled = playbackItemState != null && playbackItemState.maxTimeInMilliseconds > DEFAULT_MIN_PROGRESS
        val max = if (enabled) playbackItemState.maxTimeInMilliseconds.toInt() else DEFAULT_MAX_PROGRESS
        val progress = if (enabled) playbackItemState.currentTimeInMilliseconds.toInt() else DEFAULT_MIN_PROGRESS
        binding.slider.isEnabled = enabled
        moveSliderTo(max.toFloat(), progress.toFloat())
        binding.tvElapsed.text = if (enabled) formatTime(progress, max) else INDETERMINATE_TIME
        binding.tvTotalTime.text = if (enabled) formatTime(max, max) else INDETERMINATE_TIME
    }

    private fun moveSliderTo(valueTo: Float, value: Float) {
        binding.slider.run {
            if (this.valueTo != valueTo) {
                this.value = valueFrom
                this.valueTo = valueTo
            }
            this.value = value.coerceIn(valueFrom, this.valueTo)
        }
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
