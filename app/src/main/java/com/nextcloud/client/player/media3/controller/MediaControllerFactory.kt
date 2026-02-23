/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.controller

import androidx.media3.session.MediaController

interface MediaControllerFactory {
    suspend fun create(controllerListener: MediaController.Listener): MediaController
}
