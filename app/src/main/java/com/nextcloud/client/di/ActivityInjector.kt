/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.di

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import dagger.android.AndroidInjection

class ActivityInjector : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is Injectable) {
            AndroidInjection.inject(activity)
        }
        if (activity is FragmentActivity) {
            val fm = activity.supportFragmentManager
            fm.registerFragmentLifecycleCallbacks(FragmentInjector(), true)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        // unused atm
    }

    override fun onActivityResumed(activity: Activity) {
        // unused atm
    }

    override fun onActivityPaused(activity: Activity) {
        // unused atm
    }

    override fun onActivityStopped(activity: Activity) {
        // unused atm
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // unused atm
    }

    override fun onActivityDestroyed(activity: Activity) {
        // unused atm
    }
}
