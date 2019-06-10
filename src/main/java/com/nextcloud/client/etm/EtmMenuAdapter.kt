/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.etm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

class EtmMenuAdapter(
    context: Context,
    val onItemClicked: (Int) -> Unit
) : RecyclerView.Adapter<EtmMenuAdapter.PageViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)
    var pages: List<EtmMenuEntry> = listOf()
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

    override fun getItemCount(): Int {
        return pages.size
    }
}
