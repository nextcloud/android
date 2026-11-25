/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.theme

import android.content.Context
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerBranding @Inject constructor(private val context: Context) {

    private val capability: OCCapability
        get() = CapabilityUtils.getCapability(context)

    fun themingEnabled(): Boolean = !capability.serverColor.isNullOrEmpty()

    fun getDefaultDisplayNameForRootFolder(): String {
        if (MainApp.isOnlyOnDevice) return context.getString(R.string.drawer_item_on_device)

        return capability.serverName?.takeIf { it.isNotEmpty() }
            ?: context.getString(R.string.default_display_name_for_root_folder)
    }
}
