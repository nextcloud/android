/*
 *
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2021 Álvaro Brey Vilas
 * Copyright (C) 2021 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.owncloud.android.databinding.UnifiedSearchFooterBinding
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection

class UnifiedSearchFooterViewHolder(
    val binding: UnifiedSearchFooterBinding,
    val context: Context,
    private val listInterface: UnifiedSearchListInterface,
) :
    SectionedViewHolder(binding.root) {

    fun bind(section: UnifiedSearchSection) {
        binding.unifiedSearchFooterLayout.setOnClickListener {
            listInterface.onLoadMoreClicked(section.providerID)
        }
    }
}
