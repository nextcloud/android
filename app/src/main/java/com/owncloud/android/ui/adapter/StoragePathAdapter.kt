/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2019 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.StoragePathItemBinding
import com.owncloud.android.ui.adapter.StoragePathAdapter.StoragePathViewHolder
import com.owncloud.android.utils.theme.ViewThemeUtils

class StoragePathAdapter(
    private val pathList: List<StoragePathItem>?,
    private val storagePathAdapterListener: StoragePathAdapterListener,
    private val viewThemeUtils: ViewThemeUtils
) : RecyclerView.Adapter<StoragePathViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoragePathViewHolder {
        return StoragePathViewHolder(
            StoragePathItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: StoragePathViewHolder, position: Int) {
        if (pathList != null && pathList.size > position) {
            val storagePathItem = pathList[position]
            holder.binding.btnStoragePath.setIconResource(storagePathItem.icon)
            holder.binding.btnStoragePath.text = storagePathItem.name
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(holder.binding.btnStoragePath)
        }
    }

    override fun getItemCount(): Int {
        return pathList?.size ?: 0
    }

    interface StoragePathAdapterListener {
        /**
         * sets the chosen path.
         *
         * @param path chosen path
         */
        fun chosenPath(path: String)
    }

    inner class StoragePathViewHolder(var binding: StoragePathItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ),
        View.OnClickListener {
        init {
            binding.root.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val path = pathList?.get(absoluteAdapterPosition)?.path
            path?.let {
                storagePathAdapterListener.chosenPath(it)
            }
        }
    }
}
