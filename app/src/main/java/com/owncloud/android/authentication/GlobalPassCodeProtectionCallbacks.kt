/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.nextcloud.client.onboarding.OnboardingService
import com.nmc.android.ui.LauncherActivity
import com.owncloud.android.lib.common.utils.Log_OC

class GlobalPassCodeProtectionCallbacks(
    private val onboarding: OnboardingService,
    private val passCodeManager: PassCodeManager
) : ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.logLifecycle("onCreate(Bundle) starting")
        onboarding.launchActivityIfNeeded(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        activity.logLifecycle("onStart() starting")
    }

    override fun onActivityResumed(activity: Activity) {
        activity.logLifecycle("onResume() starting")
        if (activity !is LauncherActivity) {
            passCodeManager.onActivityResumed(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        activity.logLifecycle("onPause() ending")
    }

    override fun onActivityStopped(activity: Activity) {
        activity.logLifecycle("onStop() ending")
        if (activity !is LauncherActivity) {
            passCodeManager.onActivityStopped(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        activity.logLifecycle("onSaveInstanceState(Bundle) starting")
    }

    override fun onActivityDestroyed(activity: Activity) {
        activity.logLifecycle("onDestroy() ending")
    }

    private fun Activity.logLifecycle(message: String) {
        Log_OC.d(javaClass.simpleName, message)
    }
}
