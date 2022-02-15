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
package com.nextcloud.client.logger.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.logger.LogEntry
import com.owncloud.android.R
import java.text.SimpleDateFormat
import java.util.Locale

class LogsAdapter(context: Context) : RecyclerView.Adapter<LogsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val header = view.findViewById<TextView>(R.id.log_entry_list_item_header)
        val message = view.findViewById<TextView>(R.id.log_entry_list_item_message)
    }

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val inflater = LayoutInflater.from(context)

    var entries: List<LogEntry> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(inflater.inflate(R.layout.log_entry_list_item, parent, false))

    override fun getItemCount() = entries.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reversedPosition = entries.size - position - 1
        val entry = entries[reversedPosition]
        val header = "${timestampFormat.format(entry.timestamp)} ${entry.level.tag} ${entry.tag}"
        holder.header.text = header
        holder.message.text = entry.message
    }
}
