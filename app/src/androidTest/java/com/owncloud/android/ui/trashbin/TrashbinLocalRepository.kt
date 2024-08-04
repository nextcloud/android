/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.trashbin

import com.owncloud.android.R
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.ui.trashbin.TrashbinRepository.LoadFolderCallback

class TrashbinLocalRepository(private val testCase: TrashbinActivityIT.TestCase) : TrashbinRepository {
    override fun emptyTrashbin(callback: TrashbinRepository.OperationCallback?) {
        TODO("Not yet implemented")
    }

    override fun restoreFile(file: TrashbinFile?, callback: TrashbinRepository.OperationCallback?) {
        TODO("Not yet implemented")
    }

    override fun removeTrashbinFile(file: TrashbinFile?, callback: TrashbinRepository.OperationCallback?) {
        TODO("Not yet implemented")
    }

    @Suppress("MagicNumber")
    override fun getFolder(remotePath: String?, callback: LoadFolderCallback?) {
        when (testCase) {
            TrashbinActivityIT.TestCase.ERROR -> callback?.onError(R.string.trashbin_loading_failed)
            TrashbinActivityIT.TestCase.FILES -> {
                val files = ArrayList<TrashbinFile>()
                files.add(
                    TrashbinFile(
                        "test.png",
                        "image/png",
                        "/trashbin/test.png",
                        "subFolder/test.png",
                        // random date
                        1395847838,
                        // random date
                        1395847908
                    )
                )
                files.add(
                    TrashbinFile(
                        "image.jpg",
                        "image/jpeg",
                        "/trashbin/image.jpg",
                        "image.jpg",
                        // random date
                        1395841858,
                        // random date
                        1395837858
                    )
                )
                files.add(
                    TrashbinFile(
                        "folder",
                        "DIR",
                        "/trashbin/folder/",
                        "folder",
                        // random date
                        1395347858,
                        // random date
                        1395849858
                    )
                )

                callback?.onSuccess(files)
            }
            TrashbinActivityIT.TestCase.EMPTY -> callback?.onSuccess(ArrayList<TrashbinFile>())
        }
    }
}
