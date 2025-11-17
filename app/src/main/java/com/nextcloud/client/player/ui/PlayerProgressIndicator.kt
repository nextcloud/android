/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.toPlaybackFile
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.owncloud.android.datamodel.OCFile
import dagger.android.HasAndroidInjector
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

class PlayerProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : LinearProgressIndicator(context, attrs, defStyleAttr),
    PlaybackModel.Listener {

    @Inject
    lateinit var playbackModel: PlaybackModel

    private var playbackFile: PlaybackFile? = null

    init {
        indicatorTrackGapSize = 0
        trackStopIndicatorSize = 0
        if (!isInEditMode) {
            (context.applicationContext as HasAndroidInjector).androidInjector().inject(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            renderCurrentState()
            playbackModel.addListener(this)
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            playbackModel.removeListener(this)
        }
        visibility = GONE
        super.onDetachedFromWindow()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        val itemState = state.currentItemState
        render(itemState)
    }

    fun setFile(file: OCFile) {
        playbackFile = file.toPlaybackFile()
        renderCurrentState()
    }

    private fun renderCurrentState() {
        val itemState = playbackModel.state.getOrNull()?.currentItemState
        render(itemState)
    }

    private fun render(itemState: PlaybackItemState?) {
        if (itemState != null &&
            itemState.playerState != PlayerState.COMPLETED &&
            itemState.file.id == playbackFile?.id
        ) {
            max = itemState.maxTimeInMilliseconds.toInt()
            progress = itemState.currentTimeInMilliseconds.toInt()
            visibility = VISIBLE
        } else {
            visibility = GONE
        }
    }
}
