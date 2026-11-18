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

    // repeats the same file again and again
    REPEAT_ONE(R.drawable.player_ic_repeat_one),

    // plays all files in order, then starts over from the first one
    REPEAT_ALL(R.drawable.player_ic_repeat_all),

    // plays all files in random order, then starts over
    SHUFFLE(R.drawable.player_ic_shuffle);

    fun next(): PlayMode = when (this) {
        REPEAT_ONE -> REPEAT_ALL
        REPEAT_ALL -> SHUFFLE
        SHUFFLE -> REPEAT_ONE
    }
}
