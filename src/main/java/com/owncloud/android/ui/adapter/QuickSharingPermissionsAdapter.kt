/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.ItemQuickSharePermissionsBinding
import com.owncloud.android.datamodel.QuickPermissionModel

class QuickSharingPermissionsAdapter(
    private val quickPermissionList: MutableList<QuickPermissionModel>,
    private val onPermissionChangeListener: QuickSharingPermissionViewHolder.OnPermissionChangeListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemQuickSharePermissionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickSharingPermissionViewHolder(binding, binding.root, onPermissionChangeListener)
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
        val onPermissionChangeListener: OnPermissionChangeListener
    ) :
        RecyclerView
        .ViewHolder(itemView) {

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
