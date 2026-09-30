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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile

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

    fun scaleUp(intent: Intent, sourceView: View?) {
        val sharedElementName = sourceView?.let { ViewCompat.getTransitionName(it) }
        if (sourceView == null || sharedElementName == null) {
            activity.startActivity(intent)
            return
        }

        intent.putExtra(EXTRA_HAS_SHARED_ELEMENT, true)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sourceView, sharedElementName)
        activity.startActivity(intent, options.toBundle())
    }

    fun finishWithScaleDown() {
        activity.supportFinishAfterTransition()
    }

    companion object {
        const val EXTRA_HAS_SHARED_ELEMENT = "HAS_SHARED_ELEMENT"
        private const val SHARED_ELEMENT_NAME_PREFIX = "file_"

        fun sharedElementName(file: OCFile): String = SHARED_ELEMENT_NAME_PREFIX + file.fileId
    }
}
