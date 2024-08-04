/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dagger.android.support.AndroidSupportInjection

internal class FragmentInjector : FragmentManager.FragmentLifecycleCallbacks() {
    override fun onFragmentPreAttached(fragmentManager: FragmentManager, fragment: Fragment, context: Context) {
        super.onFragmentPreAttached(fragmentManager, fragment, context)
        if (fragment is Injectable) {
            try {
                AndroidSupportInjection.inject(fragment)
            } catch (directCause: IllegalArgumentException) {
                // this provides a cause description that is a bit more friendly for developers
                throw InjectorNotFoundException(fragment, directCause)
            }
        }
    }
}
