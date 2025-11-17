/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.state

import com.nextcloud.client.player.model.file.PlaybackFile
import java.io.Serializable

data class PlaybackState(
    val currentFiles: List<PlaybackFile>,
    val currentItemState: PlaybackItemState?,
    val repeatMode: RepeatMode,
    val shuffle: Boolean
) : Serializable
