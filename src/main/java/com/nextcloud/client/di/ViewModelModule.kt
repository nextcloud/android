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
package com.nextcloud.client.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextcloud.client.etm.EtmViewModel
import com.nextcloud.client.logger.ui.LogsViewModel
import com.owncloud.android.ui.activities.ActivitiesViewModel
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(EtmViewModel::class)
    abstract fun etmViewModel(vm: EtmViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LogsViewModel::class)
    abstract fun logsViewModel(vm: LogsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UnifiedSearchViewModel::class)
    abstract fun unifiedSearchViewModel(vm: UnifiedSearchViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
