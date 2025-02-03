/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ionos.annotation.IonosCustomization
import com.owncloud.android.databinding.ItemQuickSharePermissionsBinding
import com.owncloud.android.datamodel.QuickPermissionModel
import com.owncloud.android.utils.theme.ViewThemeUtils

class QuickSharingPermissionsAdapter(
    private val quickPermissionList: MutableList<QuickPermissionModel>,
    private val onPermissionChangeListener: QuickSharingPermissionViewHolder.OnPermissionChangeListener,
    private val viewThemeUtils: ViewThemeUtils
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemQuickSharePermissionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickSharingPermissionViewHolder(binding, binding.root, onPermissionChangeListener, viewThemeUtils)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is QuickSharingPermissionViewHolder) {
            holder.bindData(quickPermissionList[position])
        }
    }

    override fun getItemCount(): Int {
        return quickPermissionList.size
    }

    class QuickSharingPermissionViewHolder(
        val binding: ItemQuickSharePermissionsBinding,
        itemView: View,
        val onPermissionChangeListener: OnPermissionChangeListener,
        private val viewThemeUtils: ViewThemeUtils
    ) :
        RecyclerView
            .ViewHolder(itemView) {

        @IonosCustomization("Disable icon tinting")
        fun bindData(quickPermissionModel: QuickPermissionModel) {
            binding.tvQuickShareName.text = quickPermissionModel.permissionName
            if (quickPermissionModel.isSelected) {
                binding.tvQuickShareCheckIcon.visibility = View.VISIBLE
            } else {
                binding.tvQuickShareCheckIcon.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener {
                // if user select different options then only update the permission
                if (!quickPermissionModel.isSelected) {
                    onPermissionChangeListener.onPermissionChanged(adapterPosition)
                } else {
                    // dismiss sheet on selection of same permission
                    onPermissionChangeListener.onDismissSheet()
                }
            }
        }

        interface OnPermissionChangeListener {
            fun onPermissionChanged(position: Int)
            fun onDismissSheet()
        }
    }
}
