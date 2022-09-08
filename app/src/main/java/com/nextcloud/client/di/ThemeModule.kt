/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.di

import android.content.Context
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.owncloud.android.utils.theme.ThemeAvatarUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import com.owncloud.android.utils.theme.ThemeDrawableUtils
import com.owncloud.android.utils.theme.ThemeSnackbarUtils
import com.owncloud.android.utils.theme.ThemeTextUtils
import com.owncloud.android.utils.theme.ThemeToolbarUtils
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.newm3.MaterialSchemesProvider
import com.owncloud.android.utils.theme.newm3.MaterialSchemesProviderImpl
import com.owncloud.android.utils.theme.newm3.ViewThemeUtils
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal abstract class ThemeModule {

    @Binds
    abstract fun bindMaterialSchemesProvider(provider: MaterialSchemesProviderImpl): MaterialSchemesProvider

    companion object {

        @Provides
        @Singleton
        fun themeColorUtils(): ThemeColorUtils {
            return ThemeColorUtils()
        }

        @Provides
        @Singleton
        fun themeToolbarUtils(
            themeColorUtils: ThemeColorUtils?,
            themeDrawableUtils: ThemeDrawableUtils?,
            viewThemeUtils: ViewThemeUtils
        ): ThemeToolbarUtils {
            return ThemeToolbarUtils(themeColorUtils, themeDrawableUtils, viewThemeUtils)
        }

        @Provides
        @Singleton
        fun themeDrawableUtils(context: Context?): ThemeDrawableUtils {
            return ThemeDrawableUtils(context)
        }

        @Provides
        @Singleton
        fun themeUtils(): ThemeUtils {
            return ThemeUtils()
        }

        @Provides
        @Singleton
        fun themeSnackbarUtils(): ThemeSnackbarUtils {
            return ThemeSnackbarUtils()
        }

        @Provides
        @Singleton
        fun themeTextUtils(): ThemeTextUtils {
            return ThemeTextUtils()
        }

        @Provides
        @Singleton
        fun themeAvatarUtils(): ThemeAvatarUtils {
            return ThemeAvatarUtils()
        }

        @Provides
        fun provideMaterialSchemes(materialSchemesProvider: MaterialSchemesProvider): MaterialSchemes {
            return materialSchemesProvider.getMaterialSchemesForCurrentUser()
        }
    }
}
