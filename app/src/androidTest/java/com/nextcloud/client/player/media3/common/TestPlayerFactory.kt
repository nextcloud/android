/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.common

import android.os.Looper
import androidx.media3.common.Player

class TestPlayerFactory : PlayerFactory {

    override fun create(): Player = TestPlayer(Looper.getMainLooper())
}
