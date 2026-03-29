/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@ichibi.eu>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.owncloud.android.databinding.UserInfoDetailsTableItemBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class UserInfoAdapter(val mDisplayList: MutableList<UserInfoDetailsItem>, val viewThemeUtils: ViewThemeUtils) :
    RecyclerView.Adapter<UserInfoAdapter.ViewHolder>() {

    class UserInfoDetailsItem(
        @field:DrawableRes @param:DrawableRes var icon: Int,
        var text: String?,
        var iconContentDescription: String?
    )

    class ViewHolder(var binding: UserInfoDetailsTableItemBinding) :
        RecyclerView.ViewHolder(binding.getRoot())

    @SuppressLint("NotifyDataSetChanged")
    fun setData(displayList: MutableList<UserInfoDetailsItem>) {
        mDisplayList.clear()
        mDisplayList.addAll(displayList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            UserInfoDetailsTableItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mDisplayList[position]
        holder.binding.icon.setImageResource(item.icon)
        holder.binding.text.text = item.text
        holder.binding.icon.contentDescription = item.iconContentDescription
        viewThemeUtils.platform.colorImageView(holder.binding.icon, ColorRole.PRIMARY)
    }

    override fun getItemCount(): Int {
        return mDisplayList.size
    }
}