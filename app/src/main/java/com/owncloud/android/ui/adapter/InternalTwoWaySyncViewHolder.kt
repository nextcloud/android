/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.InternalTwoWaySyncViewHolderBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.DisplayUtils

class InternalTwoWaySyncViewHolder(val binding: InternalTwoWaySyncViewHolderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(folder: OCFile, context: Context) {

        binding.apply {
            name.text = folder.decryptedFileName
            if (folder.internalFolderSyncTimestamp == 0L) {
                syncTimestamp.text = context.getString(R.string.internal_two_way_sync_not_yet)
            } else {
                syncTimestamp.text = DisplayUtils.unixTimeToHumanReadable(folder.internalFolderSyncTimestamp)
            }
        }
    }
}
