/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.PredefinedStatusBinding
import com.owncloud.android.lib.resources.users.PredefinedStatus

class PredefinedStatusListAdapter(private val clickListener: PredefinedStatusClickListener, val context: Context) :
    RecyclerView.Adapter<PredefinedStatusViewHolder>() {
    internal var list: List<PredefinedStatus> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredefinedStatusViewHolder {
        val itemBinding = PredefinedStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PredefinedStatusViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: PredefinedStatusViewHolder, position: Int) {
        holder.bind(list[position], clickListener, context)
    }

    override fun getItemCount(): Int = list.size
}
