/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var widgets = emptyList<DashboardWidget>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return WidgetListItemViewHolder(
            WidgetListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            accountManager,
            clientFactory,
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val widgetListItemViewHolder = holder as WidgetListItemViewHolder

        widgetListItemViewHolder.bind(widgets[position], dashboardWidgetConfigurationInterface)
    }

    override fun getItemCount(): Int {
        return widgets.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setWidgetList(list: Map<String, DashboardWidget>?) {
        widgets = list?.map { (_, value) -> value }?.sortedBy { it.order } ?: emptyList()
        notifyDataSetChanged()
    }
}
