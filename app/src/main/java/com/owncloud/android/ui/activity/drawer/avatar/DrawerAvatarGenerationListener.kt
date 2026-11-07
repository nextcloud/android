/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.avatar

import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.nextcloud.utils.avatar.AvatarGenerationListener
import com.owncloud.android.lib.common.utils.Log_OC

class DrawerAvatarGenerationListener : AvatarGenerationListener {

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        when (callContext) {
            is MenuItem -> {
                val newIcon = callContext.setIcon(avatarDrawable)
                Log_OC.d(TAG, "avatarGenerated new icon: $newIcon")
            }

            is ImageView -> callContext.setImageDrawable(avatarDrawable)

            is MaterialButton -> callContext.icon = avatarDrawable
        }
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        val contextTag: Any? = when (callContext) {
            is MenuItem -> callContext.title
            is ImageView -> callContext.tag
            is MaterialButton -> callContext.tag
            else -> return false
        }
        return contextTag.toString() == tag
    }

    companion object {
        private const val TAG = "DrawerAvatarGenerationListener"
    }
}
