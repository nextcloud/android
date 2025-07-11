/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.logger.LogEntry
import com.owncloud.android.R
import java.text.SimpleDateFormat
import java.util.Locale

class LogsAdapter(private val context: Context) : RecyclerView.Adapter<LogsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val header = view.findViewById<TextView>(R.id.log_entry_list_item_header)
        val message = view.findViewById<TextView>(R.id.log_entry_list_item_message)
    }

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val inflater = LayoutInflater.from(context)

    var entries: List<LogEntry> = listOf()
        @SuppressLint("NotifyDataSetChanged")
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
        val entryColor = ContextCompat.getColor(context, entry.level.getColor())

        holder.header.setTextColor(entryColor)
        holder.header.text = header

        holder.message.setTextColor(entryColor)
        holder.message.text = entry.message
    }
}
