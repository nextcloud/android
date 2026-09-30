/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation.animator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.owncloud.android.R

class NavigationAnimator(private val activity: AppCompatActivity) {

    fun slideUp(intent: Intent) {
        val options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.slide_up, R.anim.hold)
        activity.startActivity(intent, options.toBundle())
    }

    fun slideDown(intent: Intent) {
        val options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.slide_down, R.anim.hold)
        activity.startActivity(intent, options.toBundle())
    }

}
