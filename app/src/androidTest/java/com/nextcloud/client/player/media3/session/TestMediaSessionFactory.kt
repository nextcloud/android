/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.session

import android.content.Context
import androidx.media3.session.MediaSession
import com.nextcloud.client.player.media3.common.PlayerFactory
import java.util.UUID

class TestMediaSessionFactory(private val context: Context, private val playerFactory: PlayerFactory) :
    MediaSessionFactory {

    override fun create(): MediaSession {
        val player = playerFactory.create()
        return MediaSession.Builder(context, player)
            .setId(UUID.randomUUID().toString())
            .build()
    }
}
