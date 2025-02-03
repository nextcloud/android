/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ionos.annotation.IonosCustomization
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

    @IonosCustomization()
    override fun onBindViewHolder(holder: StoragePathViewHolder, position: Int) {
        if (pathList != null && pathList.size > position) {
            val storagePathItem = pathList[position]
            holder.binding.btnStoragePath.setIconResource(storagePathItem.icon)
            holder.binding.btnStoragePath.text = storagePathItem.name
            viewThemeUtils.ionos.material.colorMaterialButtonPrimaryBorderless(holder.binding.btnStoragePath)
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
