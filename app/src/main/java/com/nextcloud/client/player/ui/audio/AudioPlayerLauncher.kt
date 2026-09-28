/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.media3.PlaybackQueueLoader
import com.nextcloud.client.player.model.file.PlaybackCollection
import com.owncloud.android.datamodel.OCFile
import javax.inject.Inject

class AudioPlayerLauncher @Inject constructor(
    private val playbackQueueLoader: PlaybackQueueLoader,
    private val playbackModel: PlaybackModel
) {
    fun launch(activity: AppCompatActivity, file: OCFile, collection: PlaybackCollection) {
        playbackQueueLoader.load(activity, file, collection) {
            playbackModel.play()
            activity.startActivity(AudioPlayerActivity.createIntent(activity))
        }
    }
}
