/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activities.adapter

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.databinding.VersionListItemBinding
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.files.model.FileVersion
import com.owncloud.android.ui.interfaces.ActivityListInterface
import com.owncloud.android.ui.interfaces.VersionListInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Date

class ActivityAndVersionListAdapter(
    context: FragmentActivity,
    currentAccountProvider: CurrentAccountProvider,
    activityListInterface: ActivityListInterface,
    private val versionListInterface: VersionListInterface.View,
    viewThemeUtils: ViewThemeUtils
) : ActivityListAdapter(context, currentAccountProvider, activityListInterface, true, viewThemeUtils) {

    @SuppressLint("NotifyDataSetChanged")
    fun setActivityAndVersionItems(items: MutableList<Any?>, newClient: NextcloudClient?, clear: Boolean) {
        if (client == null) client = newClient

        if (clear) {
            values.clear()
            items.sortByDescending { it.timestamp() }
        }

        var sTime = ""
        for (item in items) {
            val time = getHeaderDateString(context, item.timestamp() ?: continue).toString()
            if (!sTime.equals(time, ignoreCase = true)) {
                sTime = time
                values.add(sTime)
            }
            if (item != null) {
                values.add(item)
            }
        }

        notifyDataSetChanged()
    }

    private fun Any?.timestamp(): Long? = when (this) {
        is Activity -> datetime.time
        is FileVersion -> modifiedTimestamp
        else -> null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VERSION_TYPE) {
            return VersionViewHolder(VersionListItemBinding.inflate(LayoutInflater.from(parent.context)))
        }
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VersionViewHolder) {
            val fileVersion = values[position] as FileVersion
            holder.binding.size.text = DisplayUtils.bytesToHumanReadable(fileVersion.fileLength)
            holder.binding.time.text = DateFormat.format("HH:mm", Date(fileVersion.modifiedTimestamp).time)
            holder.binding.restore.setOnClickListener { versionListInterface.onRestoreClicked(fileVersion) }
        } else {
            super.onBindViewHolder(holder, position)
        }
    }

    override fun getItemViewType(position: Int) = when (values[position]) {
        is Activity -> ACTIVITY_TYPE
        is FileVersion -> VERSION_TYPE
        else -> HEADER_TYPE
    }

    open class VersionViewHolder(val binding: VersionListItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val VERSION_TYPE = 102
    }
}
