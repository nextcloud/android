/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.storagePermissionBanner

import android.app.Activity
import android.view.View
import com.nextcloud.utils.BuildHelper.isFlavourGPlay
import com.nextcloud.utils.extensions.openAllFilesAccessSettings
import com.nextcloud.utils.extensions.openMediaPermissions
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.MainApp
import com.owncloud.android.databinding.StoragePermissionWarningBannerBinding
import com.owncloud.android.ui.activity.DrawerActivity.REQ_ALL_FILES_ACCESS
import com.owncloud.android.ui.activity.DrawerActivity.REQ_MEDIA_ACCESS
import com.owncloud.android.utils.PermissionUtil

fun StoragePermissionWarningBannerBinding.setup(activity: Activity, descriptionId: Int) {
    description.text = activity.getString(descriptionId)

    val isBrandedAndFlavourGplay = (MainApp.isClientBranded() && isFlavourGPlay())
    fullFileAccess.setVisibleIf(!PermissionUtil.checkFullFileAccess() && !isBrandedAndFlavourGplay)
    fullFileAccess.setOnClickListener { activity.openAllFilesAccessSettings(REQ_ALL_FILES_ACCESS) }

    mediaReadOnly.setVisibleIf(!PermissionUtil.checkMediaAccess(activity))
    mediaReadOnly.setOnClickListener { activity.openMediaPermissions(REQ_MEDIA_ACCESS) }

    root.visibility = if (PermissionUtil.checkFullFileAccess() || PermissionUtil.checkMediaAccess(activity)) {
        View.GONE
    } else {
        View.VISIBLE
    }
}
