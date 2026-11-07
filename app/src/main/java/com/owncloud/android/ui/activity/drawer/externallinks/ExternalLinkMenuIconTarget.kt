/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.externallinks

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class ExternalLinkMenuIconTarget(private val onIconLoaded: (Drawable?) -> Unit) : CustomTarget<Drawable>() {

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        onIconLoaded(resource)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        onIconLoaded(errorDrawable)
    }

    override fun onLoadCleared(placeholder: Drawable?) = Unit
}
