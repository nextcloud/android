/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.controller

import androidx.media3.session.MediaController

interface MediaControllerFactory {
    suspend fun create(controllerListener: MediaController.Listener): MediaController
}
