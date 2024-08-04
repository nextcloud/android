/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.owncloud.android.datamodel.OCFile

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
}
