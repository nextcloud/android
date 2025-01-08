/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.owncloud.android.databinding.UnifiedSearchHeaderBinding
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.utils.theme.ViewThemeUtils

class UnifiedSearchHeaderViewHolder(
    val binding: UnifiedSearchHeaderBinding,
    val viewThemeUtils: ViewThemeUtils,
    val context: Context
) : SectionedViewHolder(binding.root) {

    fun bind(section: UnifiedSearchSection) {
        binding.title.text = section.name
        viewThemeUtils.platform.colorPrimaryTextViewElement(binding.title)
    }
}
