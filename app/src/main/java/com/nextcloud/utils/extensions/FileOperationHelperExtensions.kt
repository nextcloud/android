/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.View
import androidx.annotation.IdRes
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.helpers.FileOperationsHelper

fun FileOperationsHelper.handle(@IdRes itemId: Int, file: OCFile, view: View?): Boolean {
    when (itemId) {
        R.id.action_send_file -> sendShareFile(file, true)
        R.id.action_stream_media -> streamMediaFile(file)
        R.id.action_set_as_wallpaper -> setPictureAs(file, view)
        R.id.action_encrypted -> toggleEncryption(file, true)
        R.id.action_unset_encrypted -> toggleEncryption(file, false)
        R.id.action_lock_file -> toggleFileLock(file, true)
        R.id.action_unlock_file -> toggleFileLock(file, false)
        else -> return false
    }

    return true
}

fun FileOperationsHelper.handle(
    @IdRes itemId: Int,
    files: Collection<OCFile>,
    view: View?,
    backgroundJobManager: BackgroundJobManager
): Boolean {
    when (itemId) {
        R.id.action_export_file -> exportFiles(files, view?.context, view, backgroundJobManager)
        R.id.action_add_to_album -> addFileToAlbum(files)
        else -> return false
    }

    return true
}
