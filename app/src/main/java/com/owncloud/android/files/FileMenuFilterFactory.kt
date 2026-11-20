/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter
import javax.inject.Inject

class FileMenuFilterFactory @Inject constructor(
    private val storageManager: FileDataStorageManager,
    private val context: Context,
    private val editorUtils: EditorUtils
) {
    fun newInstance(
        numberOfAllFiles: Int,
        files: Collection<OCFile>,
        componentsGetter: ComponentsGetter,
        overflowMenu: Boolean,
        user: User
    ) = FileMenuFilter(
        storageManager,
        editorUtils,
        numberOfAllFiles,
        files,
        componentsGetter,
        context,
        overflowMenu,
        user
    )

    fun newInstance(file: OCFile, componentsGetter: ComponentsGetter, overflowMenu: Boolean, user: User) =
        newInstance(FileSelection.SINGLE_SELECT_ITEMS, listOf(file), componentsGetter, overflowMenu, user)
}
