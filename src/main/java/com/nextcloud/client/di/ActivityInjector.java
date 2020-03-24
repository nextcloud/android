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

package com.nextcloud.client.di;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import dagger.android.AndroidInjection;

public class ActivityInjector implements Application.ActivityLifecycleCallbacks {

    @Override
    public final void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof Injectable) {
            AndroidInjection.inject(activity);
        }

        if (activity instanceof FragmentActivity) {
            final FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
            fm.registerFragmentLifecycleCallbacks(new FragmentInjector(), true);
        }
    }

    @Override
    public final void onActivityStarted(Activity activity) {
        // not needed
    }

    @Override
    public final void onActivityResumed(Activity activity) {
        // not needed
    }

    @Override
    public final void onActivityPaused(Activity activity) {
        // not needed
    }

    @Override
    public final void onActivityStopped(Activity activity) {
        // not needed
    }

    @Override
    public final void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // not needed
    }

    @Override
    public final void onActivityDestroyed(Activity activity) {
        // not needed
    }
}
