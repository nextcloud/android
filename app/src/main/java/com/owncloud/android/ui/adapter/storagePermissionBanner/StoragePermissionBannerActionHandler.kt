/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.storagePermissionBanner

import android.view.View
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.openAllFilesAccessSettings
import com.nextcloud.utils.extensions.openMediaPermissions
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.StoragePermissionWarningBannerBinding
import com.owncloud.android.utils.PermissionUtil

fun StoragePermissionWarningBannerBinding.setup(appPreferences: AppPreferences) {
    val context = this.root.context

    fullFileAccess.setVisibleIf(!PermissionUtil.checkFullFileAccess())
    fullFileAccess.setOnClickListener { context.openAllFilesAccessSettings() }

    mediaReadOnly.setVisibleIf(!PermissionUtil.checkMediaAccess(context))
    mediaReadOnly.setOnClickListener { context.openMediaPermissions() }

    dontShowStoragePermissionBanner.setOnClickListener {
        root.visibility = View.GONE
        appPreferences.setShowStoragePermissionBanner(false)
    }

    root.visibility = if (PermissionUtil.checkFullFileAccess() || PermissionUtil.checkMediaAccess(context)) {
        View.GONE
    } else {
        View.VISIBLE
    }
}
