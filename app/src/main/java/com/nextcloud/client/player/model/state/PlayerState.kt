/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.state

import java.io.Serializable

enum class PlayerState : Serializable {
    IDLE,
    PLAYING,
    PAUSED,
    COMPLETED,
    NONE
}
