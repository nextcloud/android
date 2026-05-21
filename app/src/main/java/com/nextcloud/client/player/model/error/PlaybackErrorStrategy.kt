/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.error

import com.nextcloud.client.player.model.state.PlaybackState
import java.io.Serializable

interface PlaybackErrorStrategy : Serializable {
    fun switchToNextSource(error: Throwable, state: PlaybackState): Boolean
}
