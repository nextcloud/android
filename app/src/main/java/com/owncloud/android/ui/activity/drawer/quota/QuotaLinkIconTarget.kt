/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.quota

import android.graphics.drawable.Drawable
import android.widget.TextView
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class QuotaLinkIconTarget(private val iconSize: Int, private val quotaTextLink: TextView) :
    CustomTarget<Drawable>() {

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        setIcon(resource)
    }

    override fun onLoadCleared(placeholder: Drawable?) = Unit

    override fun onLoadFailed(errorDrawable: Drawable?) {
        super.onLoadFailed(errorDrawable)
        errorDrawable?.let(::setIcon)
    }

    private fun setIcon(drawable: Drawable) {
        val icon = drawable.current.apply { setBounds(0, 0, iconSize, iconSize) }
        quotaTextLink.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }
}
