/* Nextcloud Android client application
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
package com.nextcloud.client.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.features.FeatureItem
import com.owncloud.android.ui.activity.PassCodeActivity

internal class OnboardingServiceImpl constructor(
    private val resources: Resources,
    private val preferences: AppPreferences,
    private val accountProvider: CurrentAccountProvider
) : OnboardingService {

    private companion object {
        const val ITEM_VERSION_CODE = 99999999
    }

    private val notSeenYet: Boolean
        get() {
            return BuildConfig.VERSION_CODE >= ITEM_VERSION_CODE && preferences.lastSeenVersionCode < ITEM_VERSION_CODE
        }

    override val whatsNew: Array<FeatureItem>
        get() = if (!isFirstRun && notSeenYet) {
            emptyArray()
        } else {
            emptyArray()
        }

    override val isFirstRun: Boolean
        get() {
            return accountProvider.currentAccount == null
        }

    override fun shouldShowWhatsNew(callingContext: Context): Boolean {
        return callingContext !is PassCodeActivity && whatsNew.size > 0
    }

    override fun launchActivityIfNeeded(activity: Activity) {
        if (!resources.getBoolean(R.bool.show_whats_new) || activity is WhatsNewActivity) {
            return
        }

        if (shouldShowWhatsNew(activity)) {
            activity.startActivity(Intent(activity, WhatsNewActivity::class.java))
        }
    }

    override fun launchFirstRunIfNeeded(activity: Activity): Boolean {
        val isProviderOrOwnInstallationVisible = resources.getBoolean(R.bool.show_provider_or_own_installation)
        val canLaunch = isProviderOrOwnInstallationVisible && isFirstRun && activity is AuthenticatorActivity
        if (canLaunch) {
            val intent = Intent(activity, FirstRunActivity::class.java)
            activity.startActivityForResult(intent, AuthenticatorActivity.REQUEST_CODE_FIRST_RUN)
        }
        return canLaunch
    }
}
