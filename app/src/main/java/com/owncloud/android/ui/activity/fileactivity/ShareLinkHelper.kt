/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import android.content.Intent
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ShareLinkToDialog
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.MimeType

object ShareLinkHelper {
    private const val TAG = "ShareLinkHelper"

    fun copyAndShareFileLink(activity: FileActivity, file: OCFile, link: String?) {
        if (!MDMConfig.shareViaLink(activity) || !MDMConfig.clipBoardSupport(activity)) return

        ClipboardUtil.copyToClipboard(activity, link, false)
        Snackbar
            .make(activity.findViewById(android.R.id.content), R.string.clipboard_text_copied, Snackbar.LENGTH_LONG)
            .setAction(R.string.share) { showShareLinkDialog(activity, file, link) }
            .also { activity.viewThemeUtils.material.themeSnackbar(it) }
            .show()
    }

    fun showShareLinkDialog(activity: FileActivity, file: ServerFileInterface, link: String?) {
        val username = activity.sharingUsername()
        val subject = if (username != null) {
            activity.getString(R.string.subject_user_shared_with_you, username, file.fileName)
        } else {
            activity.getString(R.string.subject_shared_with_you, file.fileName)
        }

        val intentToShareLink = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, link)
            type = MimeType.TEXT_PLAIN
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        ShareLinkToDialog
            .newInstance(intentToShareLink, activity.packageName)
            .show(activity.supportFragmentManager, FileDisplayActivity.FTAG_CHOOSER_DIALOG)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun FileActivity.sharingUsername(): String? {
        val displayName = try {
            OwnCloudAccount(account, this).displayName
        } catch (e: Exception) {
            Log_OC.e(TAG, "Could not read account display name", e)
            null
        }
        return displayName.takeUnless { it.isNullOrEmpty() } ?: AccountUtils.getUsernameForAccount(account)
    }
}
