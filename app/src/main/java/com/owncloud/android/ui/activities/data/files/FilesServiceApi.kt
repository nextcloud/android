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

/**
 * Defines an interface to the Files service API. All {[OCFile]} remote data requests
 * should be piped through this interface.
 */
interface FilesServiceApi {
    interface FilesServiceCallback<T> {
        fun onLoaded(ocFile: OCFile)
        fun onError(error: String)
    }

    fun readRemoteFile(fileUrl: String, activity: BaseActivity, callback: FilesServiceCallback<OCFile>)
}
