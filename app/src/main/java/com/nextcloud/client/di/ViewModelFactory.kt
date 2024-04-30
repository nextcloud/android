/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        var vmProvider: Provider<ViewModel>? = viewModelProviders[modelClass]

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

        @Suppress("UNCHECKED_CAST")
        return vmProvider.get() as T
    }
}
