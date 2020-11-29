/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.trashbin

import com.owncloud.android.R
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.ui.trashbin.TrashbinRepository.LoadFolderCallback

class TrashbinLocalRepository(val testCase: TrashbinActivityIT.TestCase) : TrashbinRepository {
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
                val files = ArrayList<Any>()
                files.add(
                    TrashbinFile(
                        "test.png",
                        "image/png",
                        "/trashbin/test.png",
                        "subFolder/test.png",
                        1395847838, // random date
                        1395847908 // random date
                    )
                )
                files.add(
                    TrashbinFile(
                        "image.jpg",
                        "image/jpeg",
                        "/trashbin/image.jpg",
                        "image.jpg",
                        1395841858, // random date
                        1395837858 // random date
                    )
                )
                files.add(
                    TrashbinFile(
                        "folder",
                        "DIR",
                        "/trashbin/folder/",
                        "folder",
                        1395347858, // random date
                        1395849858 // random date
                    )
                )

                callback?.onSuccess(files)
            }
            TrashbinActivityIT.TestCase.EMPTY -> callback?.onSuccess(ArrayList<Any>())
        }
    }
}
