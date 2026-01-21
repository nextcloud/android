/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.composeActivity

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ComposeViewModel : ViewModel() {
    private val _selectedText = MutableStateFlow<String?>(null)
    val selectedText: StateFlow<String?> = _selectedText

    fun updateSelectedText(value: String) {
        _selectedText.update {
            value
        }
    }
}
