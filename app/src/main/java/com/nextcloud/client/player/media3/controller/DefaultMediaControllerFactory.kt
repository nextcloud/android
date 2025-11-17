/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nextcloud.client.player.media3.PlaybackService
import kotlinx.coroutines.guava.await
import javax.inject.Inject

@UnstableApi
class DefaultMediaControllerFactory @Inject constructor(private val context: Context) : MediaControllerFactory {

    override suspend fun create(controllerListener: MediaController.Listener): MediaController {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        return MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
            .await()
    }
}
