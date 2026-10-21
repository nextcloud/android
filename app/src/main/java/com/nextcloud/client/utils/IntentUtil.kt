/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ShareLinkToDialog

object IntentUtil {

    @JvmStatic
    public fun createSendIntent(context: Context, file: OCFile): Intent = createBaseSendFileIntent().apply {
        action = Intent.ACTION_SEND
        type = file.mimeType
        putExtra(Intent.EXTRA_STREAM, file.getExposedFileUri(context))
    }

    @JvmStatic
    public fun createSendIntent(context: Context, files: Array<OCFile>): Intent = createBaseSendFileIntent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = getUniqueMimetype(files)
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, getExposedFileUris(context, files))
    }

    private fun createBaseSendFileIntent(): Intent = Intent().apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun getUniqueMimetype(files: Array<OCFile>): String? = when {
        files.distinctBy { it.mimeType }.size > 1 -> "*/*"
        else -> files[0].mimeType
    }

    private fun getExposedFileUris(context: Context, files: Array<OCFile>): ArrayList<Uri> =
        ArrayList(files.map { it.getExposedFileUri(context) })

    @JvmStatic
    fun startLinkIntent(activity: Activity, @StringRes link: Int) = startLinkIntent(activity, activity.getString(link))

    @JvmStatic
    fun startLinkIntent(activity: Activity, url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }

        startLinkIntent(activity, url.toUri())
    }

    @JvmStatic
    fun startLinkIntent(activity: Activity, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startIntentIfAppAvailable(intent, activity, R.string.no_browser_available)
    }

    @JvmStatic
    fun startIntentIfAppAvailable(intent: Intent, activity: Activity, @StringRes error: Int) {
        if (intent.resolveActivity(activity.packageManager) == null) {
            SnackbarUtil.show(activity, error)
            return
        }

        activity.startActivity(intent)
    }

    fun showShareLinkDialog(activity: FragmentActivity, link: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, link)
            setType("text/plain")
        }

        ShareLinkToDialog.newInstance(intent, activity.packageName).run {
            show(activity.supportFragmentManager, FileDisplayActivity.FTAG_CHOOSER_DIALOG)
        }
    }
}
