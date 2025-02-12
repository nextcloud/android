/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.owncloud.android.datamodel.OCFile

class OCFileDiffCallback(private val oldList: List<OCFile>, private val newList: List<OCFile>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].remoteId === newList[newItemPosition].remoteId ||
            oldList[oldItemPosition].decryptedRemotePath === newList[newItemPosition].decryptedRemotePath
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return oldList[oldPosition].remoteId == newList[newPosition].remoteId &&
            oldList[oldPosition].hashCode() == newList[newPosition].hashCode() &&
            oldList[oldPosition].fileLength == newList[newPosition].fileLength
    }
}
