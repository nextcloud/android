/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.lib.resources.dashboard.DashboardWidget
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.widget.DashboardWidgetConfigurationInterface
import com.nextcloud.utils.GlideHelper
import com.owncloud.android.R
import com.owncloud.android.databinding.WidgetListItemBinding

class WidgetListItemViewHolder(
    val binding: WidgetListItemBinding,
    val accountManager: UserAccountManager,
    val clientFactory: ClientFactory,
    val context: Context
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        dashboardWidget: DashboardWidget,
        dashboardWidgetConfigurationInterface: DashboardWidgetConfigurationInterface
    ) {
        binding.layout.setOnClickListener { dashboardWidgetConfigurationInterface.onItemClicked(dashboardWidget) }

        GlideHelper.loadViaURISVGIntoImageView(context, dashboardWidget.iconUrl, binding.icon, R.drawable.ic_dashboard)

        binding.name.text = dashboardWidget.title
    }
}
