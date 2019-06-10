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
import javax.inject.Inject
import javax.inject.Provider

/**
 * This factory provide [ViewModel] instances initialized by Dagger 2 dependency injection system.
 *
 * Each [javax.inject.Provider] instance accesses Dagger machinery, which provide
 * fully-initialized [ViewModel] instance.
 *
 * @see ViewModelModule
 * @see ViewModelKey
 */
class ViewModelFactory @Inject constructor(
    private val viewModelProviders: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        var vmProvider: Provider<ViewModel>? = viewModelProviders.get(modelClass)

        if (vmProvider == null) {
            for (entry in viewModelProviders.entries) {
                if (modelClass.isAssignableFrom(entry.key)) {
                    vmProvider = entry.value
                    break
                }
            }
        }

        if (vmProvider == null) {
            throw IllegalArgumentException("${modelClass.simpleName} view model class is not supported")
        }

        @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "UNCHECKED_CAST")
        try {
            val vm = vmProvider.get() as T
            return vm
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
