/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.composeActivity

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.owncloud.android.utils.theme.CapabilityUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComposeProcessTextAlias @Inject constructor(private val context: Context) {

    fun configure() {
        val capability = CapabilityUtils.getCapability(context)
        val isAssistantAvailable = capability.assistant.isTrue

        val componentName = ComponentName(
            context,
            "com.nextcloud.ui.composeActivity.ComposeProcessTextAlias"
        )

        val newState = if (isAssistantAvailable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        context.packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
}
