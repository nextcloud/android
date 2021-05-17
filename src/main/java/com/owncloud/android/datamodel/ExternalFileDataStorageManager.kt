/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
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

package com.owncloud.android.datamodel

import android.accounts.Account
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.RemoteException
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.common.utils.Log_OC

class ExternalFileDataStorageManager(account: Account, contentResolver: ContentResolver) :
    FileDataStorageManager(account, contentResolver) {

    // fun getFileIdByPath(path: String?): Long {
    //     val cursor = getFileCursorForValue(ProviderTableMeta.FILE_PATH, path!!)
    //     return if (cursor!!.moveToFirst()) {
    //         getLong(cursor, ProviderTableMeta._ID)
    //     } else {
    //         -1
    //     }
    // }

    override fun getFileCursorForValue(key: String, value: String): Cursor? {
        val cursor: Cursor?
        cursor = if (contentResolver != null) {
            contentResolver
                .query(
                    ProviderTableMeta.EXTERNAL_CONTENT_URI,
                    null,
                    key + AND
                        + ProviderTableMeta.FILE_ACCOUNT_OWNER
                        + "=?", arrayOf(value, account.name), null
                )
        } else {
            try {
                contentProviderClient.query(
                    ProviderTableMeta.EXTERNAL_CONTENT_URI,
                    null,
                    key + AND + ProviderTableMeta.FILE_ACCOUNT_OWNER
                        + "=?", arrayOf(value, account.name),
                    null
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Could not get file details: " + e.message, e)
                null
            }
        }
        return cursor
    }

    override fun getFolderContent(parentId: Long, onlyOnDevice: Boolean): List<OCFile>? {
        val folderContent: MutableList<OCFile> = ArrayList()
        val requestURI = Uri.withAppendedPath(ProviderTableMeta.EXTERNAL_CONTENT_URI_DIR, parentId.toString())
        val cursor: Cursor?
        cursor = if (contentProviderClient != null) {
            try {
                contentProviderClient.query(
                    requestURI,
                    null,
                    ProviderTableMeta.FILE_PARENT + "=?", arrayOf(parentId.toString()),
                    null
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, e.message, e)
                return folderContent
            }
        } else {
            contentResolver.query(
                requestURI,
                null,
                ProviderTableMeta.FILE_PARENT + "=?", arrayOf(parentId.toString()),
                null
            )
        }
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val child = createFileInstance(cursor)
                    if (!onlyOnDevice || child.existsOnDevice()) {
                        folderContent.add(child)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return folderContent
    }

    companion object {
        val TAG = ExternalFileDataStorageManager::class.java.simpleName
    }
}
