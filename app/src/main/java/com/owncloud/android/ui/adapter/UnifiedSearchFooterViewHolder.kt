/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.owncloud.android.databinding.UnifiedSearchFooterBinding
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.utils.theme.ViewThemeUtils

class UnifiedSearchFooterViewHolder(
    val binding: UnifiedSearchFooterBinding,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val listInterface: UnifiedSearchListInterface
) : SectionedViewHolder(binding.root) {

    fun bind(section: UnifiedSearchSection) {
        viewThemeUtils.material.colorMaterialTextButton(binding.unifiedSearchFooterLayout)
        binding.unifiedSearchFooterLayout.setOnClickListener {
            listInterface.onLoadMoreClicked(section.providerID)
        }
    }
}
