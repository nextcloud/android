/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.User
import com.owncloud.android.databinding.InternalTwoWaySyncViewHolderBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

class InternalTwoWaySyncAdapter(
    private val dataStorageManager: FileDataStorageManager,
    private val user: User,
    val context: Context,
    private val onUpdateListener: InternalTwoWaySyncAdapterOnUpdate
) : RecyclerView.Adapter<InternalTwoWaySyncViewHolder>() {

    interface InternalTwoWaySyncAdapterOnUpdate {
        fun onUpdate(folderSize: Int)
    }

    var folders: List<OCFile> = dataStorageManager.getInternalTwoWaySyncFolders(user)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InternalTwoWaySyncViewHolder =
        InternalTwoWaySyncViewHolder(
            InternalTwoWaySyncViewHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = folders.size

    override fun onBindViewHolder(holder: InternalTwoWaySyncViewHolder, position: Int) {
        holder.bind(folders[position], context, dataStorageManager, this)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update() {
        folders = dataStorageManager.getInternalTwoWaySyncFolders(user)
        notifyDataSetChanged()
        onUpdateListener.onUpdate(folders.size)
    }
}
