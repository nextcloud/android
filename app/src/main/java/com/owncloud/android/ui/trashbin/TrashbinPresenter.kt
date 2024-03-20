/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.trashbin

import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.ui.trashbin.TrashbinContract.Presenter
import com.owncloud.android.ui.trashbin.TrashbinRepository.LoadFolderCallback
import java.io.File

/**
 * Coordinates between model and view: querying model, updating view, react to UI input
 */
class TrashbinPresenter(
    private val trashbinRepository: TrashbinRepository,
    private val trashbinView: TrashbinContract.View
) : Presenter {

    private var currentPath: String? = OCFile.ROOT_PATH

    override fun enterFolder(folder: String?) {
        currentPath = folder
        loadFolder()
    }

    override val isRoot: Boolean
        get() = OCFile.ROOT_PATH == currentPath

    override fun navigateUp() {
        if (isRoot) {
            trashbinView.close()
        } else {
            currentPath?.let {
                currentPath = File(it).parent
                loadFolder()
            }
        }
    }

    override fun loadFolder() {
        trashbinRepository.getFolder(
            currentPath,
            object : LoadFolderCallback {
                override fun onSuccess(files: List<TrashbinFile?>?) {
                    trashbinView.showTrashbinFolder(files)
                }

                override fun onError(error: Int) {
                    trashbinView.showError(error)
                }
            }
        )
        trashbinView.atRoot(isRoot)
    }

    override fun restoreTrashbinFile(file: TrashbinFile?) {
        trashbinRepository.restoreFile(
            file,
            object : TrashbinRepository.OperationCallback {
                override fun onResult(success: Boolean) {
                    if (success) {
                        trashbinView.removeFile(file)
                    } else {
                        trashbinView.showSnackbarError(R.string.trashbin_file_not_restored, file)
                    }
                }
            }
        )
    }

    override fun removeTrashbinFile(file: TrashbinFile?) {
        trashbinRepository.removeTrashbinFile(
            file,
            object : TrashbinRepository.OperationCallback {
                override fun onResult(success: Boolean) {
                    if (success) {
                        trashbinView.removeFile(file)
                    } else {
                        trashbinView.showSnackbarError(R.string.trashbin_file_not_deleted, file)
                    }
                }
            }
        )
    }

    override fun emptyTrashbin() {
        trashbinRepository.emptyTrashbin(object : TrashbinRepository.OperationCallback {
            override fun onResult(success: Boolean) {
                if (success) {
                    trashbinView.removeAllFiles()
                } else {
                    trashbinView.showError(R.string.trashbin_not_emptied)
                }
            }
        })
    }
}
