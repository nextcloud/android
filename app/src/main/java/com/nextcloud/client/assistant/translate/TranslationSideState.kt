/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.translate

import com.owncloud.android.lib.resources.assistant.v2.model.TranslationLanguage

data class TranslationSideState(
    val text: String = "",
    val language: TranslationLanguage? = null,
    val isExpanded: Boolean = false
)
