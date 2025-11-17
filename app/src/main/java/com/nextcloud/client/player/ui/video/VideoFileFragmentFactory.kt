/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.video

import androidx.fragment.app.Fragment
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.ui.pager.PlayerPagerFragmentFactory

class VideoFileFragmentFactory : PlayerPagerFragmentFactory<PlaybackFile> {

    override fun create(item: PlaybackFile): Fragment = VideoFileFragment.createInstance(item)
}
