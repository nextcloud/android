/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.fileactions

import android.content.Context
import android.view.ActionMode
import android.view.View
import androidx.fragment.app.FragmentManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.preview.media.PreviewMediaActivity

class FileActionHost @JvmOverloads constructor(
    val containerActivity: FileFragment.ContainerActivity,
    val fragmentManager: FragmentManager,
    val view: View?,
    val parentFolder: OCFile?,
    val actionMode: ActionMode? = null,
    val onFileOpened: () -> Unit = {},
    val onFavoriteChanged: (isFavorite: Boolean) -> Unit = {}
) {
    val helper: FileOperationsHelper?
        get() = containerActivity.fileOperationsHelper

    val fileActivity: FileActivity?
        get() = containerActivity as? FileActivity

    val fda: FileDisplayActivity?
        get() = containerActivity as? FileDisplayActivity

    val previewMediaActivity: PreviewMediaActivity?
        get() = containerActivity as? PreviewMediaActivity

    val context: Context?
        get() = view?.context
}
