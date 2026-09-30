/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation.animator

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.owncloud.android.R

class NavigationAnimator(private val activity: AppCompatActivity) {

    fun slideUp(intent: Intent) {
        val options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.slide_up, R.anim.hold)
        activity.startActivity(intent, options.toBundle())
    }

    fun finishWithSlideDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.hold, R.anim.slide_down)
            activity.finish()
            return
        }

        activity.finish()
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.hold, R.anim.slide_down)
    }
}
