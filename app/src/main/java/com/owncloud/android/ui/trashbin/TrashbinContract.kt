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
