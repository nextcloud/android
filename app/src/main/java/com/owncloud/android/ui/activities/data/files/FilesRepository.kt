/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.BaseActivity

interface FilesRepository {
    interface ReadRemoteFileCallback {
        fun onFileLoaded(ocFile: OCFile?)
        fun onFileLoadError(error: String)
    }

    fun readRemoteFile(path: String, activity: BaseActivity, callback: ReadRemoteFileCallback)
}
