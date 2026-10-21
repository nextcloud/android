/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation.animator

import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback

class SharedElementTransition(
    private val activity: AppCompatActivity,
    private val startScaleType: ImageView.ScaleType? = null,
    private val sharedViewProvider: () -> View?
) {
    private val decorView: View
        get() = activity.window.decorView

    private val startPostponedTransition: Runnable = Runnable {
        decorView.viewTreeObserver.removeOnPreDrawListener(startWhenSharedViewReady)
        decorView.removeCallbacks(startPostponedTransition)
        activity.supportStartPostponedEnterTransition()
    }

    private val startWhenSharedViewReady: ViewTreeObserver.OnPreDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (sharedViewProvider() != null) {
            startPostponedTransition.run()
        }
        true
    }

    fun register() {
        activity.setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                val name = names.firstOrNull() ?: return
                val sharedView = sharedViewProvider()
                if (sharedView == null) {
                    names.clear()
                    sharedElements.clear()
                    return
                }
                sharedElements[name] = sharedView
            }

            override fun onSharedElementStart(
                names: MutableList<String>,
                sharedElements: MutableList<View>,
                snapshots: MutableList<View>
            ) {
                sharedElements.forEach {
                    it.background = null
                    if (it is ImageView && startScaleType != null) {
                        it.scaleType = startScaleType
                    }
                }
            }
        })
    }

    fun postponeUntilSharedViewReady() {
        activity.supportPostponeEnterTransition()
        decorView.viewTreeObserver.addOnPreDrawListener(startWhenSharedViewReady)
        decorView.postDelayed(startPostponedTransition, MAX_POSTPONE_MS)
    }

    companion object {
        private const val MAX_POSTPONE_MS = 500L
    }
}
