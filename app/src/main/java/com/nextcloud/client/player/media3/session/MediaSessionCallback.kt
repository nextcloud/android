/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.session

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nextcloud.client.player.media3.resumption.PlaybackResumptionLauncher
import com.nextcloud.client.player.model.PlaybackModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.guava.future
import javax.inject.Inject
import javax.inject.Provider

@UnstableApi
class MediaSessionCallback @Inject constructor(
    private val playbackModelProvider: Provider<PlaybackModel>,
    private val playbackResumptionLauncherProvider: Provider<PlaybackResumptionLauncher>
) : MediaSession.Callback {
    private val playbackModel get() = playbackModelProvider.get()
    private val playbackResumptionLauncher get() = playbackResumptionLauncherProvider.get()

    companion object {
        const val CLOSE_ACTION = "CLOSE_ACTION"
    }

    /**
     * The result of [super.onConnect] carries no available player commands since media3 1.11.0, so the connection has
     * to be built from [ConnectionResult.AcceptedResultBuilder] instead. Forwarding the commands of the super result
     * leaves the controller unable to change media items, prepare or play.
     */
    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): ConnectionResult {
        val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS
            .buildUpon()
            .add(SessionCommand(CLOSE_ACTION, Bundle.EMPTY))
            .build()
        return ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands)
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction == CLOSE_ACTION) {
            playbackModel.release()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> = GlobalScope.future { playbackResumptionLauncher.launch() }
}
