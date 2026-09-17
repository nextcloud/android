/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.state

import java.util.Objects

data class PlaybackItemMetadata(
    val title: CharSequence,
    val artist: CharSequence? = null,
    val album: CharSequence? = null,
    val genre: CharSequence? = null,
    val year: Int? = null,
    val description: CharSequence? = null,
    val artworkData: ByteArray? = null,
    val artworkUri: CharSequence? = null
) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is PlaybackItemMetadata -> false
        else -> describedFields() == other.describedFields() && artworkData.contentEquals(other.artworkData)
    }

    override fun hashCode(): Int = Objects.hash(describedFields(), artworkData?.contentHashCode())

    private fun describedFields(): List<Any?> = listOf(title, artist, album, genre, year, description, artworkUri)
}
