/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Intent
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity

@Suppress("ReturnCount")
fun DrawerActivity.handleBackButtonEvent(currentDir: OCFile): Boolean {
    if (DrawerActivity.menuItemId == R.id.nav_all_files && currentDir.isRootDirectory) {
        moveTaskToBack(true)
        return true
    }

    val isParentDirExists = (storageManager.getFileById(currentDir.parentId) != null)
    if (isParentDirExists) {
        return false
    }

    DrawerActivity.menuItemId = R.id.nav_all_files
    setNavigationViewItemChecked()

    MainApp.showOnlyFilesOnDevice(false)
    MainApp.showOnlyPersonalFiles(false)

    Intent(applicationContext, FileDisplayActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        action = FileDisplayActivity.ALL_FILES
    }.run {
        startActivity(this)
    }

    return true
}
