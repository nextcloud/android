/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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

    override fun loadFolder(onCompleted: () -> Unit, onError: () -> Unit) {
        trashbinRepository.getFolder(
            currentPath,
            object : LoadFolderCallback {
                override fun onSuccess(files: List<TrashbinFile?>?) {
                    trashbinView.showTrashbinFolder(files)
                    onCompleted()
                }

                override fun onError(error: Int) {
                    trashbinView.showError(error)
                    onError()
                }
            }
        )
        trashbinView.atRoot(isRoot)
    }

    override fun restoreTrashbinFile(files: Collection<TrashbinFile?>) {
        for (file in files) {
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
    }

    override fun removeTrashbinFile(files: Collection<TrashbinFile?>) {
        for (file in files) {
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
