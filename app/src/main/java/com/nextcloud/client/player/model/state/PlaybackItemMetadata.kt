/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.state

import java.io.Serializable

data class PlaybackItemMetadata(
    val title: CharSequence,
    val artist: CharSequence? = null,
    val album: CharSequence? = null,
    val genre: CharSequence? = null,
    val year: Int? = null,
    val description: CharSequence? = null,
    val artworkData: ByteArray? = null,
    val artworkUri: CharSequence? = null
) : Serializable
