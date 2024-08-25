/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey Vilas <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND AGPL-3.0-or-later
 */
package com.owncloud.android.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.MediaControlBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Formatter
import java.util.Locale
import javax.inject.Inject

/**
 * View containing controls for a MediaPlayer.
 *
 *
 * Holds buttons "play / pause", "rewind", "fast forward" and a progress slider.
 *
 *
 * It synchronizes itself with the state of the MediaPlayer.
 */
class MediaControlView(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs),
    View.OnClickListener,
    OnSeekBarChangeListener {

    private var playerControl: Player? = null
    private var binding: MediaControlBinding
    private var isDragging = false

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    public override fun onFinishInflate() {
        super.onFinishInflate()
    }

    @Suppress("MagicNumber")
    fun setMediaPlayer(player: Player?) {
        playerControl = player
        handler.sendEmptyMessage(SHOW_PROGRESS)

        handler.postDelayed({
            updatePausePlay()
            setProgress()
        }, 100)
    }

    fun stopMediaPlayerMessages() {
        handler.removeMessages(SHOW_PROGRESS)
    }

    @Suppress("MagicNumber")
    private fun initControllerView() {
        binding.playBtn.requestFocus()

        binding.playBtn.setOnClickListener(this)
        binding.forwardBtn.setOnClickListener(this)
        binding.rewindBtn.setOnClickListener(this)

        binding.progressBar.run {
            viewThemeUtils.platform.themeHorizontalSeekBar(this)
            setMax(1000)
        }

        binding.progressBar.setOnSeekBarChangeListener(this)

        viewThemeUtils.material.run {
            colorMaterialButtonPrimaryTonal(binding.rewindBtn)
            colorMaterialButtonPrimaryTonal(binding.playBtn)
            colorMaterialButtonPrimaryTonal(binding.forwardBtn)
        }
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private fun disableUnsupportedButtons() {
        try {
            if (playerControl!!.isCommandAvailable(Player.COMMAND_PLAY_PAUSE).not()) {
                binding.playBtn.isEnabled = false
            }

            if (playerControl!!.isCommandAvailable(Player.COMMAND_SEEK_BACK).not()) {
                binding.rewindBtn.isEnabled = false
            }
            if (playerControl!!.isCommandAvailable(Player.COMMAND_SEEK_FORWARD).not()) {
                binding.forwardBtn.isEnabled = false
            }
        } catch (ex: IncompatibleClassChangeError) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
            Log_OC.i(TAG, "Old media interface detected")
        }
    }

    @Suppress("MagicNumber")
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == SHOW_PROGRESS) {
                updatePausePlay()
                val pos = setProgress()

                if (!isDragging) {
                    sendMessageDelayed(obtainMessage(SHOW_PROGRESS), (1000 - pos % 1000).toLong())
                }
            }
        }
    }

    init {
        MainApp.getAppComponent().inject(this)

        val inflate = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = MediaControlBinding.inflate(inflate, this, true)
        initControllerView()
        isFocusable = true
        setFocusableInTouchMode(true)
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS)
        requestFocus()
    }

    @Suppress("MagicNumber")
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val mFormatBuilder = StringBuilder()
        val mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    @Suppress("MagicNumber")
    private fun setProgress(): Long {
        var position = 0L
        if (playerControl == null || isDragging) {
            position = 0
        }

        playerControl?.let { playerControl ->
            position = playerControl.currentPosition
            val duration = playerControl.duration
            if (duration > 0) {
                // use long to avoid overflow
                val pos = 1000L * position / duration
                binding.progressBar.progress = pos.toInt()
            }
            val percent = playerControl.bufferedPercentage
            binding.progressBar.setSecondaryProgress(percent * 10)
            val endTime = if (duration > 0) formatTime(duration) else "--:--"
            binding.totalTimeText.text = endTime
            binding.currentTimeText.text = formatTime(position)
        }

        return position
    }

    @Suppress("ReturnCount")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val uniqueDown = (event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN)

        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                if (uniqueDown) {
                    doPauseResume()
                    // show(sDefaultTimeout);
                    binding.playBtn.requestFocus()
                }
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (uniqueDown && playerControl?.playWhenReady == false) {
                    playerControl?.play()
                    updatePausePlay()
                }
                return true
            }

            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_PAUSE
            -> {
                if (uniqueDown && playerControl?.playWhenReady == true) {
                    playerControl?.pause()
                    updatePausePlay()
                }
                return true
            }

            else -> return super.dispatchKeyEvent(event)
        }
    }

    fun updatePausePlay() {
        binding.playBtn.icon = ContextCompat.getDrawable(
            context,
            // isPlaying reflects if the playback is actually moving forward, If the media is buffering and it will play when ready
            // it would still return that it is not playing. So, in case of buffering it will show the pause icon which would show that
            // media is loading, when user has not paused but moved the progress to a different position this works as a buffering signal.
            if (playerControl?.isPlaying == true) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
        )
        binding.forwardBtn.visibility = if (playerControl!!.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)) {
            VISIBLE
        } else {
            INVISIBLE
        }
        binding.rewindBtn.visibility = if (playerControl!!.isCommandAvailable(Player.COMMAND_SEEK_BACK)) {
            VISIBLE
        } else {
            INVISIBLE
        }
    }

    private fun doPauseResume() {
        playerControl?.run {
            if (playWhenReady) {
                pause()
            } else {
                play()
            }
        }
        updatePausePlay()
    }

    override fun setEnabled(enabled: Boolean) {
        binding.playBtn.setEnabled(enabled)
        binding.forwardBtn.setEnabled(enabled)
        binding.rewindBtn.setEnabled(enabled)
        binding.progressBar.setEnabled(enabled)

        disableUnsupportedButtons()

        super.setEnabled(enabled)
    }

    @Suppress("MagicNumber")
    override fun onClick(v: View) {

        playerControl?.let { playerControl ->
            val playing = playerControl.playWhenReady
            val id = v.id

            when (id) {
                R.id.playBtn -> {
                    doPauseResume()
                }

                R.id.rewindBtn -> {
                    playerControl.seekBack()
                    if (!playing) {
                        playerControl.pause() // necessary in some 2.3.x devices
                    }
                    setProgress()
                }

                R.id.forwardBtn -> {
                    playerControl.seekForward()
                    if (!playing) {
                        playerControl.pause() // necessary in some 2.3.x devices
                    }

                    setProgress()
                }

                else -> {
                }
            }
        }
    }

    @Suppress("MagicNumber")
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!fromUser) {
            // We're not interested in programmatically generated changes to
            // the progress bar's position.
            return
        }

        playerControl?.let { playerControl ->
            val duration = playerControl.duration.toLong()
            val newPosition = duration * progress / 1000L
            playerControl.seekTo(newPosition)
            binding.currentTimeText.text = formatTime(newPosition)
        }
    }

    /**
     * Called in devices with touchpad when the user starts to adjust the position of the seekbar's thumb.
     *
     * Will be followed by several onProgressChanged notifications.
     */
    override fun onStartTrackingTouch(seekBar: SeekBar) {
        isDragging = true // monitors the duration of dragging
        handler.removeMessages(SHOW_PROGRESS) // grants no more updates with media player progress while dragging
    }

    /**
     * Called in devices with touchpad when the user finishes the adjusting of the seekbar.
     */
    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isDragging = false
        setProgress()
        updatePausePlay()
        handler.sendEmptyMessage(SHOW_PROGRESS) // grants future updates with media player progress
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.setClassName(MediaControlView::class.java.getName())
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.setClassName(MediaControlView::class.java.getName())
    }

    companion object {
        private val TAG = MediaControlView::class.java.getSimpleName()
        private const val SHOW_PROGRESS = 1
    }
}
