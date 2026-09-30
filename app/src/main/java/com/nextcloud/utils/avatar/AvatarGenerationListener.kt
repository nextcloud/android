/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.avatar

import android.graphics.drawable.Drawable

interface AvatarGenerationListener {
    fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?)

    fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean
}
