/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.error.SourceException
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.ui.control.PlayerControlView
import com.nextcloud.client.player.ui.pager.PlayerPager
import com.nextcloud.client.player.ui.pager.PlayerPagerFragmentFactory
import com.nextcloud.client.player.ui.pager.PlayerPagerMode
import com.nextcloud.client.player.util.WindowWrapper
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

abstract class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr),
    PlaybackModel.Listener {

    @Inject
    lateinit var playbackModel: PlaybackModel

    @get:LayoutRes
    protected abstract val layoutRes: Int

    protected abstract val fragmentFactory: PlayerPagerFragmentFactory<PlaybackFile>

    protected val activity: AppCompatActivity by lazy { context as AppCompatActivity }
    protected val windowWrapper: WindowWrapper by lazy { WindowWrapper(activity.window) }

    protected val topBar: View by lazy { findViewById(R.id.topBar) }
    protected val titleTextView: TextView by lazy { findViewById(R.id.title) }
    protected val playerPager: PlayerPager<PlaybackFile> by lazy { findViewById(R.id.playerPager) }
    protected val playerControlView: PlayerControlView by lazy { findViewById(R.id.playerControlView) }

    init {
        inflate(context, layoutRes, this)
        if (!isInEditMode) {
            inject(context)
            playerPager.initialize(activity.supportFragmentManager, PlayerPagerMode.INFINITE, fragmentFactory)
            playerPager.setPlayerPagerListener { playbackModel.switchToFile(it) }
            findViewById<View>(R.id.back).setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        }
    }

    protected abstract fun inject(context: Context)

    @CallSuper
    open fun onStart() {
        val state = playbackModel.state.getOrNull()
        if (state == null) {
            activity.finish()
            return
        }

        render(state)
        playbackModel.addListener(this)
        playerControlView.onStart()
    }

    @CallSuper
    open fun onStop() {
        playbackModel.removeListener(this)
        playerControlView.onStop()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    override fun onPlaybackError(error: Throwable) {
        if (error is SourceException) {
            DisplayUtils.showSnackMessage(this, R.string.player_error_source_not_found)
        } else {
            DisplayUtils.showSnackMessage(this, R.string.common_error_unknown)
        }
    }

    private fun render(state: PlaybackState) {
        val currentFiles = state.currentFiles
        if (state.currentFiles.isEmpty()) {
            activity.finish()
            return
        }

        if (playerPager.getItems() != currentFiles) {
            playerPager.setItems(currentFiles)
        }

        if (state.currentItemState != null) {
            val file = state.currentItemState.file
            titleTextView.text = file.getNameWithoutExtension()
            playerPager.setCurrentItem(file)
        } else {
            titleTextView.text = ""
        }
    }
}
