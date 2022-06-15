/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2021 Álvaro Brey Vilas
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.owncloud.android.datamodel.OCFile

object IntentUtil {

    @JvmStatic
    public fun createSendIntent(context: Context, file: OCFile): Intent =
        createBaseSendFileIntent().apply {
            action = Intent.ACTION_SEND
            type = file.mimeType
            putExtra(Intent.EXTRA_STREAM, file.getExposedFileUri(context))
        }

    @JvmStatic
    public fun createSendIntent(context: Context, files: Array<OCFile>): Intent =
        createBaseSendFileIntent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = getUniqueMimetype(files)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, getExposedFileUris(context, files))
        }

    private fun createBaseSendFileIntent(): Intent =
        Intent().apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun getUniqueMimetype(files: Array<OCFile>): String? = when {
        files.distinctBy { it.mimeType }.size > 1 -> "*/*"
        else -> files[0].mimeType
    }

    private fun getExposedFileUris(context: Context, files: Array<OCFile>): ArrayList<Uri> =
        ArrayList(files.map { it.getExposedFileUri(context) })
}
