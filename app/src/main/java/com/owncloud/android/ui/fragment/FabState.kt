/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.utils.extensions.slideHideBottomBehavior
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.theme.ViewThemeUtils

enum class FabState {
    HIDDEN,
    DISABLED,
    ENABLED;

    fun applyTo(fab: FloatingActionButton, viewThemeUtils: ViewThemeUtils) {
        val visible = this != HIDDEN
        if (visible) fab.show() else fab.hide()
        fab.isEnabled = this == ENABLED
        viewThemeUtils.material.themeFAB(fab)
        fab.slideHideBottomBehavior(visible)
    }

    companion object {
        fun from(folder: OCFile?, storageManager: FileDataStorageManager?, hiddenByScreen: Boolean): FabState = when {
            hiddenByScreen || folder.isReadOnly(storageManager) -> HIDDEN
            folder != null && (folder.canCreateFileAndFolder() || folder.isOfflineOperation) -> ENABLED
            else -> DISABLED
        }

        private fun OCFile?.isReadOnly(storageManager: FileDataStorageManager?): Boolean =
            this != null && storageManager?.isReadOnly(this) == true
    }
}
