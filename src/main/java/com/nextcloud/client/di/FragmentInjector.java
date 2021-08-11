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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import dagger.android.support.AndroidSupportInjection;

class FragmentInjector extends FragmentManager.FragmentLifecycleCallbacks {
    @Override
    public void onFragmentPreAttached(
        @NonNull FragmentManager fragmentManager,
        @NonNull Fragment fragment,
        @NonNull Context context
    ) {
        super.onFragmentPreAttached(fragmentManager, fragment, context);
        if (fragment instanceof Injectable) {
            try {
                AndroidSupportInjection.inject(fragment);
            } catch (IllegalArgumentException directCause) {
                // this provides a cause description that is a bit more friendly for developers
                throw new InjectorNotFoundException(fragment, directCause);
            }
        }
    }
}
