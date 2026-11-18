/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.state

import androidx.annotation.DrawableRes
import com.owncloud.android.R
import kotlinx.serialization.Serializable

@Serializable
enum class PlayMode(@param:DrawableRes val iconRes: Int) {
    SINGLE(R.drawable.player_ic_repeat_one),
    REPEAT(R.drawable.player_ic_repeat),
    SHUFFLE(R.drawable.player_ic_shuffle);

    fun next(): PlayMode = when (this) {
        SINGLE -> REPEAT
        REPEAT -> SHUFFLE
        SHUFFLE -> SINGLE
    }
}
