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

import android.accounts.Account;
import android.content.Context;
import android.graphics.Color;

import com.nextcloud.android.common.ui.util.PlatformThemeUtil;
import com.owncloud.android.R;

import static com.owncloud.android.utils.theme.CapabilityUtils.getCapability;

/**
 * Utility class with methods for theming related.
 *
 * @deprecated use material 3 Schemes and utilities from common lib instead
 */
@Deprecated
public class ThemeColorUtils {
    public int unchangedPrimaryColor(Account account, Context context) {
        try {
            return Color.parseColor(getCapability(account, context).getServerColor());
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary);
        }
    }

    public int unchangedFontColor(Context context) {
        try {
            return Color.parseColor(getCapability(context).getServerTextColor());
        } catch (Exception e) {
            if (PlatformThemeUtil.isDarkMode(context)) {
                return Color.WHITE;
            } else {
                return Color.BLACK;
            }
        }
    }
}
