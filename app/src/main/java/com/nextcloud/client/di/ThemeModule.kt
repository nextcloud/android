/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di

import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.owncloud.android.utils.theme.MaterialSchemesProvider
import com.owncloud.android.utils.theme.MaterialSchemesProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal abstract class ThemeModule {

    @Binds
    abstract fun bindMaterialSchemesProvider(provider: MaterialSchemesProviderImpl): MaterialSchemesProvider

    companion object {

        @Provides
        fun provideMaterialSchemes(materialSchemesProvider: MaterialSchemesProvider): MaterialSchemes =
            materialSchemesProvider.getMaterialSchemesForCurrentUser()
    }
}
