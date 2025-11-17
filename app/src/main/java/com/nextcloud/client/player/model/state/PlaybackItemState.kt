/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.state

import com.nextcloud.client.player.model.file.PlaybackFile
import java.io.Serializable

data class PlaybackItemState(
    val file: PlaybackFile,
    val playerState: PlayerState,
    val metadata: PlaybackItemMetadata?,
    val videoSize: VideoSize?,
    val currentTimeInMilliseconds: Long,
    val maxTimeInMilliseconds: Long
) : Serializable
