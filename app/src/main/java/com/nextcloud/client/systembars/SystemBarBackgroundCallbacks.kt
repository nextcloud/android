/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.systembars

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nmc.android.ui.LauncherActivity
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import dynamiccolor.MaterialDynamicColors
import javax.inject.Provider

class SystemBarBackgroundCallbacks(private val viewThemeUtilsProvider: Provider<ViewThemeUtils>) :
    ActivityLifecycleCallbacks {

    companion object {
        private val dynamicColor = MaterialDynamicColors()
        private val excludedActivities = listOf(
            LauncherActivity::class,
            AuthenticatorActivity::class,
            FirstRunActivity::class
        )

        private fun isExcluded(activity: Activity): Boolean =
            excludedActivities.any { it.isInstance(activity) }

        @JvmStatic
        @Suppress("ReturnCount")
        fun apply(activity: Activity, viewThemeUtils: ViewThemeUtils) {
            if (isExcluded(activity)) return

            val decorView = activity.window?.decorView as? FrameLayout ?: return
            val actionBarColor = dynamicColor.surface().getArgb(viewThemeUtils.getScheme(activity))
            val statusBarColor = statusBarColor(activity, actionBarColor)

            val existing = decorView.foreground as? SystemBarsBackgroundDrawable
            if (existing != null) {
                existing.setColors(statusBarColor, actionBarColor)
                return
            }

            decorView.foreground = SystemBarsBackgroundDrawable(decorView, statusBarColor, actionBarColor)
        }

        @ColorInt
        private fun statusBarColor(activity: Activity, @ColorInt actionBarColor: Int): Int {
            if (activity is DrawerActivity && activity.isToolbarStyleSearch) {
                return ContextCompat.getColor(activity, R.color.bg_default)
            }

            return actionBarColor
        }
    }

    override fun onActivityStarted(activity: Activity) {
        apply(activity, viewThemeUtilsProvider.get())
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
