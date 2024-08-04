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
 * Contract between view (TrashbinActivity) and presenter (TrashbinPresenter)
 */
interface TrashbinContract {
    interface View {
        fun showTrashbinFolder(trashbinFiles: List<TrashbinFile?>?)
        fun showSnackbarError(message: Int, file: TrashbinFile?)
        fun showError(message: Int)
        fun removeFile(file: TrashbinFile?)
        fun removeAllFiles()
        fun close()
        fun atRoot(isRoot: Boolean)
    }

    interface Presenter {
        val isRoot: Boolean
        fun loadFolder()
        fun navigateUp()
        fun enterFolder(folder: String?)
        fun restoreTrashbinFile(file: TrashbinFile?)
        fun removeTrashbinFile(file: TrashbinFile?)
        fun emptyTrashbin()
    }
}
