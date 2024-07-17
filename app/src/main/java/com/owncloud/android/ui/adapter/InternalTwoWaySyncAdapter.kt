/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.User
import com.owncloud.android.databinding.InternalTwoWaySyncViewHolderBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

class InternalTwoWaySyncAdapter(
    dataStorageManager: FileDataStorageManager,
    user: User,
    val context: Context
) : RecyclerView.Adapter<InternalTwoWaySyncViewHolder>() {
    var folders: List<OCFile> = dataStorageManager.getInternalTwoWaySyncFolders(user)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InternalTwoWaySyncViewHolder {
        return InternalTwoWaySyncViewHolder(
            InternalTwoWaySyncViewHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    override fun onBindViewHolder(holder: InternalTwoWaySyncViewHolder, position: Int) {
        holder.bind(folders[position], context)
    }
}
