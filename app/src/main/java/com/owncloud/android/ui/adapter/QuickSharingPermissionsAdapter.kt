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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.owncloud.android.R
import com.owncloud.android.databinding.ItemQuickSharePermissionsBinding
import com.owncloud.android.datamodel.quickPermission.QuickPermission
import com.owncloud.android.utils.theme.ViewThemeUtils

class QuickSharingPermissionsAdapter(
    private val quickPermissionList: MutableList<QuickPermission>,
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
        private val binding: ItemQuickSharePermissionsBinding,
        itemView: View,
        private val onPermissionChangeListener: OnPermissionChangeListener,
        private val viewThemeUtils: ViewThemeUtils
    ) : RecyclerView.ViewHolder(itemView) {

        fun bindData(quickPermission: QuickPermission) {
            val context = itemView.context
            val permissionName = context.getString(quickPermission.type.textId)

            binding.run {
                quickPermissionButton.text = permissionName
                quickPermissionButton.iconGravity = MaterialButton.ICON_GRAVITY_START
                quickPermissionButton.icon = ContextCompat.getDrawable(context, quickPermission.type.iconId)

                if (quickPermission.isSelected) {
                    viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(quickPermissionButton)
                }
            }

            val customPermissionName = context.getString(R.string.share_custom_permission)
            val isCustomPermission = permissionName.equals(customPermissionName, ignoreCase = true)

            itemView.setOnClickListener {
                if (isCustomPermission) {
                    onPermissionChangeListener.onCustomPermissionSelected()
                } else if (!quickPermission.isSelected) {
                    // if user select different options then only update the permission
                    onPermissionChangeListener.onPermissionChanged(absoluteAdapterPosition)
                } else {
                    // dismiss sheet on selection of same permission
                    onPermissionChangeListener.onDismissSheet()
                }
            }
        }

        interface OnPermissionChangeListener {
            fun onPermissionChanged(position: Int)
            fun onCustomPermissionSelected()
            fun onDismissSheet()
        }
    }
}
