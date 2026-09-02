/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.resumption

import com.nextcloud.client.player.model.file.PlaybackCollection
import com.nextcloud.client.player.model.file.PlaybackFileType

data class PlaybackResumptionConfig(
    val currentFileId: String,
    val folderId: Long,
    val fileType: PlaybackFileType,
    val collection: PlaybackCollection
)
