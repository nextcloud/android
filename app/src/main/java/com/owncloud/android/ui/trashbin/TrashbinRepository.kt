/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.trashbin

import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile

/**
 * Contract between presenter and model
 */
interface TrashbinRepository {
    interface LoadFolderCallback {
        fun onSuccess(files: List<TrashbinFile?>?)
        fun onError(error: Int)
    }

    interface OperationCallback {
        fun onResult(success: Boolean)
    }

    fun getFolder(remotePath: String?, callback: LoadFolderCallback?)
    fun restoreFile(file: TrashbinFile?, callback: OperationCallback?)
    fun emptyTrashbin(callback: OperationCallback?)
    fun removeTrashbinFile(file: TrashbinFile?, callback: OperationCallback?)
}
