/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

class EtmMenuAdapter(context: Context, val onItemClicked: (Int) -> Unit) :
    RecyclerView.Adapter<EtmMenuAdapter.PageViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)
    var pages: List<EtmMenuEntry> = listOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class PageViewHolder(view: View, onClick: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val primaryAction: ImageView = view.findViewById(R.id.primary_action)
        val text: TextView = view.findViewById(R.id.text)
        val secondaryAction: ImageView = view.findViewById(R.id.secondary_action)

        init {
            itemView.setOnClickListener { onClick(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = layoutInflater.inflate(R.layout.material_list_item_single_line, parent, false)
        return PageViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.primaryAction.setImageResource(page.iconRes)
        holder.text.setText(page.titleRes)
        holder.secondaryAction.setImageResource(0)
    }

    override fun getItemCount(): Int = pages.size
}
