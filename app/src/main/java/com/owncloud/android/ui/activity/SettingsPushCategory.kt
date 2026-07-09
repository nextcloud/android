/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Simon Gougeon <git@sgougeon.fr>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import com.owncloud.android.R
import com.owncloud.android.ui.ThemeableSwitchPreference
import com.owncloud.android.utils.CommonPushUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import org.unifiedpush.android.connector.UnifiedPush

object SettingsPushCategory {
    @JvmStatic
    fun SettingsActivity.setup(preferenceScreen: PreferenceScreen) {
        val preferenceCategoryPush = findPreference("push") as PreferenceCategory
        viewThemeUtils.files.themePreferenceCategory(preferenceCategoryPush)

        val fUnifiedPushEnabled: Boolean = resources.getBoolean(R.bool.unifiedpush_enabled)
        val supportsWebPush: Boolean = accountManager.allUsers.any { u ->
            CapabilityUtils.getCapability(u, this).supportsWebPush.isTrue
        }
        val nPushServices = UnifiedPush.getDistributors(this).size
        if (!fUnifiedPushEnabled || !supportsWebPush || nPushServices == 0) {
            preferenceScreen.removePreference(preferenceCategoryPush)
        } else {
            setUnifiedPushPreference(nPushServices > 1)
        }
    }

    private fun SettingsActivity.setUnifiedPushPreference(canChangeService: Boolean) {
        val unifiedPushEnabled: Boolean = preferences.isUnifiedPushEnabled
        val prefUnifiedPush = findPreference("enable_unifiedpush") as ThemeableSwitchPreference?
        val prefChangeService: Preference? = findPreference("change_unifiedpush")

        prefUnifiedPush?.isChecked = unifiedPushEnabled
        prefUnifiedPush?.setOnPreferenceClickListener { _ ->
            prefChangeService?.isEnabled = prefUnifiedPush.isChecked
            // We cant make it Gone... so we inform it is disabled
            prefChangeService?.summary = resources.getString(R.string.prefs_disabled_push_system_summary)

            preferences.setUnifiedPushEnabled(prefUnifiedPush.isChecked)
            if (prefUnifiedPush.isChecked) {
                CommonPushUtils.tryUseUnifiedPush(this, accountManager, preferences) { service ->
                    if (service != null) {
                        prefChangeService?.summary = service
                    } else {
                        prefUnifiedPush.isChecked = false
                        prefChangeService?.isEnabled = false
                    }
                }
            } else {
                CommonPushUtils.disableUnifiedPush(this, accountManager, preferences.pushToken)
            }
            false
        }

        if (canChangeService) {
            if (unifiedPushEnabled) {
                val service = UnifiedPush.getAckDistributor(this)
                prefChangeService?.summary = service ?: ""
            } else {
                prefChangeService?.summary = resources.getString(R.string.prefs_disabled_push_system_summary)
            }
            prefChangeService?.isEnabled = unifiedPushEnabled
            prefChangeService?.setOnPreferenceClickListener { _ ->
                CommonPushUtils.pickUnifiedPushDistributor(
                    this,
                    accountManager,
                    preferences.getPushToken()
                ) { service ->
                    service?.let {
                        prefChangeService.summary = service
                    }
                }
                false
            }
        } else {
            prefChangeService?.parent?.removePreference(prefChangeService)
        }
    }
}
