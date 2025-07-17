/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.lib.resources.dashboard.DashboardWidget
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.widget.DashboardWidgetConfigurationInterface
import com.owncloud.android.databinding.WidgetListItemBinding

class DashboardWidgetListAdapter(
    val accountManager: UserAccountManager,
    val clientFactory: ClientFactory,
    val context: Context,
    private val dashboardWidgetConfigurationInterface: DashboardWidgetConfigurationInterface
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var widgets = emptyList<DashboardWidget>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        WidgetListItemViewHolder(
            WidgetListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            accountManager,
            clientFactory,
            context
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val widgetListItemViewHolder = holder as WidgetListItemViewHolder

        widgetListItemViewHolder.bind(widgets[position], dashboardWidgetConfigurationInterface)
    }

    override fun getItemCount(): Int = widgets.size

    @SuppressLint("NotifyDataSetChanged")
    fun setWidgetList(list: Map<String, DashboardWidget>?) {
        widgets = list?.map { (_, value) -> value }?.sortedBy { it.order } ?: emptyList()
        notifyDataSetChanged()
    }
}
