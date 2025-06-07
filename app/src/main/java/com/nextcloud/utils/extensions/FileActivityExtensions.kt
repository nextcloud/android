/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.Bundle
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity

fun FileActivity.initFile(savedInstanceState: Bundle?): OCFile? {
    return savedInstanceState?.getParcelableArgument(FileActivity.EXTRA_FILE, OCFile::class.java)
        ?: intent.getParcelableArgument(FileActivity.EXTRA_FILE, OCFile::class.java)
        ?: storageManager.getFileById(intent.getLongExtra(FileActivity.EXTRA_FILE_ID, -1))
}
