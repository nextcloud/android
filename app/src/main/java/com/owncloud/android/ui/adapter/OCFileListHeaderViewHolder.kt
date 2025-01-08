/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.ListHeaderBinding

internal class OCFileListHeaderViewHolder(var binding: ListHeaderBinding) :
    RecyclerView.ViewHolder(
        binding.root
    ) {
    val headerText
        get() = binding.headerText

    val headerView
        get() = binding.headerView
}
