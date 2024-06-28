/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.documentscan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.owncloud.android.databinding.DocumentPageItemBinding

class DocumentPageListAdapter :
    ListAdapter<String, DocumentPageListAdapter.DocumentPageViewHolder>(DiffItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentPageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DocumentPageItemBinding.inflate(inflater, parent, false)
        return DocumentPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentPageViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    class DocumentPageViewHolder(val binding: DocumentPageItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imagePath: String) {
            binding.root.load(imagePath)
        }
    }

    private class DiffItemCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
