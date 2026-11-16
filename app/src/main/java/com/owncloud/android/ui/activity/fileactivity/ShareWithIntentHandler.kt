/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import android.content.Intent
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.providers.UsersAndGroupsSearchProvider
import com.owncloud.android.ui.activity.FileActivity

class ShareWithIntentHandler(
    private val activity: FileActivity,
    private val onNewSharee: (shareeName: String, shareType: ShareType) -> Unit
) {

    fun handle(intent: Intent, remotePath: String) {
        val data = intent.data ?: return
        val shareWith = data.toString().substringAfterLast('/')
        val shareType = UsersAndGroupsSearchProvider.getShareType(data.authority)

        val isAlreadyShared = activity.storageManager
            .getSharesWithForAFile(remotePath, activity.account.name)
            .any { shareeKey(it.shareType, it.shareWith) == shareeKey(shareType, shareWith) }

        if (isAlreadyShared) {
            SnackbarUtil.show(activity, activity.getString(R.string.sharee_already_added_to_file))
        } else {
            onNewSharee(shareWith, shareType)
        }
    }

    private fun shareeKey(shareType: ShareType?, shareWith: String?): String = "${shareType}_$shareWith"
}
