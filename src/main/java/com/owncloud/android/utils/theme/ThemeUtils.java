/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils.theme;

import android.content.Context;
import android.content.res.Configuration;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.resources.status.OCCapability;

/**
 * Utility class with methods for client side theming.
 */
public final class ThemeUtils {
    private ThemeUtils() {
        // utility class -> private constructor
    }

    public static boolean themingEnabled(Context context) {
        return CapabilityUtils.getCapability(context).getServerColor() != null
            && !CapabilityUtils.getCapability(context).getServerColor().isEmpty();
    }

    public static String getDefaultDisplayNameForRootFolder(Context context) {
        OCCapability capability = CapabilityUtils.getCapability(context);

        if (MainApp.isOnlyOnDevice()) {
            return MainApp.getAppContext().getString(R.string.drawer_item_on_device);
        } else {
            if (capability.getServerName() == null || capability.getServerName().isEmpty()) {
                return MainApp.getAppContext().getResources().getString(R.string.default_display_name_for_root_folder);
            } else {
                return capability.getServerName();
            }
        }
    }

    public static boolean isDarkModeActive(Context context) {
        int nightModeFlag = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        return Configuration.UI_MODE_NIGHT_YES == nightModeFlag;
    }
}
