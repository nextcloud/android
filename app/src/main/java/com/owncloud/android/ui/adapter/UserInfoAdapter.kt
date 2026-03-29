/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@ichibi.eu>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.owncloud.android.R
import com.owncloud.android.databinding.UserInfoDetailsTableItemBinding
import com.owncloud.android.databinding.UserInfoDetailsTableItemTitleBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class UserInfoAdapter(val context: Context, val mDisplayList: MutableList<UserInfoDetailsItem>, val viewThemeUtils: ViewThemeUtils) :
    SectionedRecyclerViewAdapter<SectionedViewHolder>() {
    companion object {
        const val SECTION_USERINFO = 0
    }

    class UserInfoDetailsItem(
        @field:DrawableRes @param:DrawableRes var icon: Int,
        var text: String?,
        var iconContentDescription: String?
    )

    class HeaderSectionedViewHolder(var binding: UserInfoDetailsTableItemTitleBinding) :
        SectionedViewHolder(binding.getRoot())

    class UserInfoSectionedViewHolder(var binding: UserInfoDetailsTableItemBinding) :
        SectionedViewHolder(binding.getRoot())

    @SuppressLint("NotifyDataSetChanged")
    fun setData(displayList: MutableList<UserInfoDetailsItem>) {
        mDisplayList.clear()
        mDisplayList.addAll(displayList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder = when (viewType) {
        VIEW_TYPE_HEADER -> HeaderSectionedViewHolder(
            UserInfoDetailsTableItemTitleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        else -> UserInfoSectionedViewHolder(
            UserInfoDetailsTableItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
        )
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        val item = mDisplayList[relativePosition]
        val isFirst = relativePosition == 0
        val isLast = relativePosition == getItemCount(section) - 1
        val uiHolder = holder as UserInfoSectionedViewHolder

        // Set background
        if (isFirst) {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_first_background)
        } else if (isLast) {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_last_background)
        } else {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_center_background)
        }

        // Populate views
        uiHolder.binding.icon.setImageResource(item.icon)
        uiHolder.binding.text.text = item.text
        uiHolder.binding.icon.contentDescription = item.iconContentDescription
        viewThemeUtils.platform.colorImageView(holder.binding.icon, ColorRole.PRIMARY)
    }

    override fun getSectionCount(): Int = 1

    override fun getItemCount(section: Int): Int = when (section) {
        SECTION_USERINFO -> mDisplayList.size
        else -> 0
    }

    override fun onBindHeaderViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        expanded: Boolean
    ) {}

    override fun onBindFooterViewHolder(p0: SectionedViewHolder?, p1: Int) {}
}