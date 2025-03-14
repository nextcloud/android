/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileSortOrder

@Suppress("TooManyFunctions")
interface CommonOCFileListAdapterInterface {
    fun isMultiSelect(): Boolean
    fun cancelAllPendingTasks()
    fun getItemPosition(file: OCFile): Int
    fun swapDirectory(
        user: User,
        directory: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        mLimitToMimeType: String
    )

    fun setHighlightedItem(file: OCFile)
    fun setSortOrder(mFile: OCFile, sortOrder: FileSortOrder)
    fun addCheckedFile(file: OCFile)
    fun isCheckedFile(file: OCFile): Boolean
    fun getCheckedItems(): Set<OCFile>
    fun removeCheckedFile(file: OCFile)
    fun notifyItemChanged(file: OCFile)
    fun getFilesCount(): Int
    fun setMultiSelect(boolean: Boolean)
    fun clearCheckedItems()
    fun selectAll(value: Boolean)
}
