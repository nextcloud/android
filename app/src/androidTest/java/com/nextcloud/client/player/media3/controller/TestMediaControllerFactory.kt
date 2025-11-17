/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.controller

import android.content.Context
import androidx.media3.session.MediaController
import com.nextcloud.client.player.media3.session.MediaSessionHolder
import kotlinx.coroutines.guava.await
import javax.inject.Provider

class TestMediaControllerFactory(
    private val context: Context,
    private val sessionHolder: Provider<MediaSessionHolder>
) : MediaControllerFactory {

    override suspend fun create(controllerListener: MediaController.Listener): MediaController =
        MediaController.Builder(context, sessionHolder.get().getMediaSession().token)
            .setListener(controllerListener)
            .buildAsync()
            .await()
}
