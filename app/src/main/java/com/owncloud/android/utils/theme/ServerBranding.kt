/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.theme;

import android.content.Context;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.resources.status.OCCapability;

/**
 * Utility class with methods for client side theming.
 */
public final class ThemeUtils {
    public boolean themingEnabled(Context context) {
        OCCapability capability = CapabilityUtils.getCapability(context);

        return capability.getServerColor() != null
            && !capability.getServerColor().isEmpty();
    }

    public String getDefaultDisplayNameForRootFolder(Context context) {
        OCCapability capability = CapabilityUtils.getCapability(context);

        if (MainApp.isOnlyOnDevice()) {
            return MainApp.string(R.string.drawer_item_on_device);
        } else {
            if (capability.getServerName() == null || capability.getServerName().isEmpty()) {
                return MainApp.getAppContext().getResources().getString(R.string.default_display_name_for_root_folder);
            } else {
                return capability.getServerName();
            }
        }
    }
}
