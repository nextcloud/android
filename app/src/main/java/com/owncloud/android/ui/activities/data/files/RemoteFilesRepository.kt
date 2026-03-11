/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activities.data.files.FilesRepository.ReadRemoteFileCallback
import com.owncloud.android.ui.activities.data.files.FilesServiceApi.FilesServiceCallback
import com.owncloud.android.ui.activity.BaseActivity

class RemoteFilesRepository(private val filesServiceApi: FilesServiceApi) : FilesRepository {
    override fun readRemoteFile(path: String, activity: BaseActivity, callback: ReadRemoteFileCallback) {
        filesServiceApi.readRemoteFile(
            path,
            activity,
            object : FilesServiceCallback<OCFile> {
                override fun onLoaded(ocFile: OCFile) {
                    callback.onFileLoaded(ocFile)
                }

                override fun onError(error: String) {
                    callback.onFileLoadError(error)
                }
            }
        )
    }
}
