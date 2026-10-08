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
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile

class NavigationAnimator(private val activity: AppCompatActivity) {

    private val isLaunchedWithSharedElement: Boolean
        get() = activity.intent.getBooleanExtra(EXTRA_HAS_SHARED_ELEMENT, false)

    fun slideUp(intent: Intent, sharedView: View? = null) {
        if (startWithSharedElement(intent, sharedView)) {
            return
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.slide_up, R.anim.hold)
        activity.startActivity(intent, options.toBundle())
    }

    fun prepareSlideUpEnter(savedInstanceState: Bundle?, sharedViewProvider: () -> View?) {
        if (!isLaunchedWithSharedElement) {
            return
        }

        activity.window.enterTransition = bottomSlide()
        activity.window.returnTransition = bottomSlide()

        val sharedElementTransition = SharedElementTransition(
            activity,
            startScaleType = ImageView.ScaleType.FIT_CENTER,
            sharedViewProvider = sharedViewProvider
        )
        sharedElementTransition.register()
        if (savedInstanceState == null) {
            sharedElementTransition.postponeUntilSharedViewReady()
        }
    }

    fun finishWithSlideDown() {
        if (isLaunchedWithSharedElement) {
            activity.setEnterSharedElementCallback(withoutSharedElements())
            activity.supportFinishAfterTransition()
            return
        }

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
        if (!startWithSharedElement(intent, sourceView)) {
            activity.startActivity(intent)
        }
    }

    fun finishWithScaleDown() {
        activity.supportFinishAfterTransition()
    }

    private fun startWithSharedElement(intent: Intent, sharedView: View?): Boolean {
        val sharedElementName = sharedView?.let { ViewCompat.getTransitionName(it) } ?: return false

        intent.putExtra(EXTRA_HAS_SHARED_ELEMENT, true)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedView, sharedElementName)
        activity.startActivity(intent, options.toBundle())
        return true
    }

    private fun withoutSharedElements() = object : SharedElementCallback() {
        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
            names.clear()
            sharedElements.clear()
        }
    }

    private fun bottomSlide() = Slide(Gravity.BOTTOM).apply {
        excludeTarget(android.R.id.statusBarBackground, true)
        excludeTarget(android.R.id.navigationBarBackground, true)
    }

    companion object {
        const val EXTRA_HAS_SHARED_ELEMENT = "HAS_SHARED_ELEMENT"
        private const val SHARED_ELEMENT_NAME_PREFIX = "file_"

        fun sharedElementName(file: OCFile): String = SHARED_ELEMENT_NAME_PREFIX + file.fileId
    }
}
