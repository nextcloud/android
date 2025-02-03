/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ionos.analycis.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Provider

class PrivacySettingsViewModel @Inject constructor(
    private val analyticsManager: AnalyticsManager,
    private val privacyPreferences: PrivacyPreferences,
) : ViewModel() {

    private var _stateFlow = MutableStateFlow(State())
    val stateFlow = _stateFlow.asStateFlow()

    fun onStart() {
        val isAnalyticsEnabled = privacyPreferences.isAnalyticsEnabled()
        _stateFlow.update { it.copy(isAnalyticsEnabled = isAnalyticsEnabled) }
    }

    fun onAnalyticsCheckedChange(isChecked: Boolean) {
        _stateFlow.update { it.copy(isAnalyticsEnabled = isChecked) }
        analyticsManager.setEnabled(isChecked)
        privacyPreferences.setAnalyticsEnabled(isChecked)
    }

    data class State(
        val isAnalyticsEnabled: Boolean = false,
    )

    class Factory @Inject constructor(
        private val viewModelProvider: Provider<PrivacySettingsViewModel>,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModelProvider.get() as T
        }
    }
}
