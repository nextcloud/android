/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@ichibi.eu>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

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

class UserInfoAdapter(val context: Context, val displayList: Map<Int, List<UserInfoDetailsItem>>, val viewThemeUtils: ViewThemeUtils) :
    SectionedRecyclerViewAdapter<SectionedViewHolder>() {
    companion object {
        const val SECTION_USERINFO = 0
        const val SECTION_GROUPS = 1
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
        val item = displayList[section]?.get(relativePosition)
        val isFirst = relativePosition == 0
        val isLast = relativePosition == getItemCount(section) - 1
        val isSingle = isFirst && isLast
        val uiHolder = holder as UserInfoSectionedViewHolder

        // Set background
        if (isSingle) {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_single_background)
        } else if (isFirst) {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_first_background)
        } else if (isLast) {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_last_background)
        } else {
            uiHolder.binding.root.setBackgroundResource(R.drawable.rounded_corners_listitem_center_background)
        }

        // Populate views
        uiHolder.binding.icon.setImageResource(item?.icon ?: R.drawable.ic_user_outline)
        uiHolder.binding.text.text = item?.text
        uiHolder.binding.icon.contentDescription = item?.iconContentDescription
        viewThemeUtils.platform.colorImageView(holder.binding.icon, ColorRole.PRIMARY)
    }

    override fun getSectionCount(): Int = 2

    override fun getItemCount(section: Int): Int = when (section) {
        SECTION_GROUPS -> displayList[SECTION_GROUPS]?.size ?: 0
        SECTION_USERINFO -> displayList[SECTION_USERINFO]?.size ?: 0
        else -> 0
    }

    override fun onBindHeaderViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        expanded: Boolean
    ) {
        val title = when (section) {
            SECTION_GROUPS -> context.getString(R.string.user_info_groups)
            SECTION_USERINFO -> context.getString(R.string.user_info_profile)
            else -> ""
        }
        val titleHolder = holder as HeaderSectionedViewHolder
        titleHolder.binding.root.text = title
        viewThemeUtils.platform.colorTextView(titleHolder.binding.root, ColorRole.PRIMARY)
    }

    override fun onBindFooterViewHolder(p0: SectionedViewHolder?, p1: Int) {}
}
