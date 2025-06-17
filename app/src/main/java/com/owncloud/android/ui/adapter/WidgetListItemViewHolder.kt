/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.nextcloud.android.lib.resources.dashboard.DashboardWidget
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.widget.DashboardWidgetConfigurationInterface
import com.owncloud.android.R
import com.owncloud.android.databinding.WidgetListItemBinding
import com.owncloud.android.utils.DisplayUtils

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

        val target = object : SimpleTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                binding.icon.setImageDrawable(resource)
                binding.icon.setColorFilter(context.getColor(R.color.dark), PorterDuff.Mode.SRC_ATOP)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                binding.icon.setImageDrawable(errorDrawable)
                binding.icon.setColorFilter(context.getColor(R.color.dark), PorterDuff.Mode.SRC_ATOP)
            }
        }

        DisplayUtils.downloadIcon(
            accountManager,
            clientFactory,
            context,
            dashboardWidget.iconUrl,
            target,
            R.drawable.ic_dashboard
        )
        binding.name.text = dashboardWidget.title
    }
}
