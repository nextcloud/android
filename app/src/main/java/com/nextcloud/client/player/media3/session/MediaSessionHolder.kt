/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.session

import androidx.media3.session.MediaSession

interface MediaSessionHolder {

    fun getMediaSession(): MediaSession

    fun release()
}
