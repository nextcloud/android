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

import com.owncloud.android.utils.theme.ThemeAvatarUtils;
import com.owncloud.android.utils.theme.ThemeBarUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeFabUtils;
import com.owncloud.android.utils.theme.ThemeLayoutUtils;
import com.owncloud.android.utils.theme.ThemeMenuUtils;
import com.owncloud.android.utils.theme.ThemeSnackbarUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;
import com.owncloud.android.utils.theme.ThemeTextUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class ThemeModule {
    @Provides
    @Singleton
    ThemeColorUtils themeColorUtils() {
        return new ThemeColorUtils();
    }

    @Provides
    @Singleton
    ThemeFabUtils themeFabUtils(ThemeColorUtils themeColorUtils, ThemeDrawableUtils themeDrawableUtils) {
        return new ThemeFabUtils(themeColorUtils, themeDrawableUtils);
    }

    @Provides
    @Singleton
    ThemeLayoutUtils themeLayoutUtils(ThemeColorUtils themeColorUtils) {
        return new ThemeLayoutUtils(themeColorUtils);
    }

    @Provides
    @Singleton
    ThemeToolbarUtils themeToolbarUtils(ThemeColorUtils themeColorUtils,
                                        ThemeDrawableUtils themeDrawableUtils,
                                        ThemeTextInputUtils themeTextInputUtils) {
        return new ThemeToolbarUtils(themeColorUtils, themeDrawableUtils, themeTextInputUtils);
    }

    @Provides
    @Singleton
    ThemeDrawableUtils themeDrawableUtils(Context context) {
        return new ThemeDrawableUtils(context);
    }

    @Provides
    @Singleton
    ThemeUtils themeUtils() {
        return new ThemeUtils();
    }

    @Provides
    @Singleton
    ThemeMenuUtils themeMenuUtils() {
        return new ThemeMenuUtils();
    }

    @Provides
    @Singleton
    ThemeSnackbarUtils themeSnackbarUtils() {
        return new ThemeSnackbarUtils();
    }

    @Provides
    @Singleton
    ThemeTextUtils themeTextUtils() {
        return new ThemeTextUtils();
    }

    @Provides
    @Singleton
    ThemeButtonUtils themeButtonUtils() {
        return new ThemeButtonUtils();
    }

    @Provides
    @Singleton
    ThemeBarUtils themeBarUtils() {
        return new ThemeBarUtils();
    }

    @Provides
    @Singleton
    ThemeTextInputUtils themeTextInputUtils() {
        return new ThemeTextInputUtils();
    }

    @Provides
    @Singleton
    ThemeCheckableUtils themeCheckableUtils() {
        return new ThemeCheckableUtils();
    }

    @Provides
    @Singleton
    ThemeAvatarUtils themeAvatarUtils() {
        return new ThemeAvatarUtils();
    }
}
