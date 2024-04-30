/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.FileDetailsShareSecureFileDropAddNewItemBinding

internal class NewSecureFileDropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var binding: FileDetailsShareSecureFileDropAddNewItemBinding? = null

    constructor(binding: FileDetailsShareSecureFileDropAddNewItemBinding) : this(binding.root) {
        this.binding = binding
    }

    fun bind(listener: ShareeListAdapterListener) {
        binding!!.addNewSecureFileDrop.setOnClickListener { v: View? -> listener.createSecureFileDrop() }
    }
}
