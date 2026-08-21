/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import android.content.Context
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.fragment.app.Fragment
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.ui.PlayerView
import com.owncloud.android.R

class AudioPlayerView(context: Context) : PlayerView(context) {

    override val layoutRes get() = R.layout.player_audio_view

    override val createFragment: (PlaybackFile) -> Fragment get() = { AudioFileFragment.createInstance(it) }

    override fun onStart() {
        super.onStart()
        windowWrapper.showSystemBars()
    }

    override fun onApplyWindowInsets(windowInsets: WindowInsets): WindowInsets? {
        val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
        val insets = windowInsetsCompat.getInsets(Type.systemBars() or Type.displayCutout())

        topBar.setPadding(insets.left, insets.top, insets.right, 0)
        playerPager.setPadding(insets.left, 0, insets.right, 0)
        playerControlView.setPadding(insets.left, 0, insets.right, insets.bottom)

        windowWrapper.setupStatusBar(R.color.player_background_color, false)
        windowWrapper.setupNavigationBar(R.color.player_background_color, true)

        return WindowInsetsCompat.CONSUMED.toWindowInsets()
    }
}
