/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.ListFooterBinding

internal class OCFileListFooterViewHolder(var binding: ListFooterBinding) :
    RecyclerView.ViewHolder(
        binding.root
    ) {
    val footerText
        get() = binding.footerText

    val loadingProgressBar
        get() = binding.loadingProgressBar
}
