/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3

import android.content.Context
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import com.nextcloud.client.player.util.PlayerUtil.playbackFile

@UnstableApi
class MediaNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {

    override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence =
        metadata.title ?: metadata.playbackFile?.getNameWithoutExtension() ?: ""

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence =
        metadata.artist ?: metadata.albumTitle ?: ""
}
