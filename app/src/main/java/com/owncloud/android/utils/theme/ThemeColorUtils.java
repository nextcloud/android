/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
