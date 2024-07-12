/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.nextcloud.utils.extensions.displayName
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission

class ContactManager(private val context: Context) {

    fun openContact(searchResult: SearchResultEntry, listInterface: UnifiedSearchListInterface) {
        val havePermission = checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        val displayName = searchResult.displayName()
        val contactId: Long? = if (havePermission && displayName != null) {
            getContactId(displayName).firstOrNull()
        } else {
            null
        }

        if (contactId == null) {
            val messageId = if (havePermission) {
                R.string.unified_search_fragment_contact_cannot_be_found_on_device
            } else {
                R.string.unified_search_fragment_permission_needed
            }
            context.showToast(messageId)
            listInterface.onSearchResultClicked(searchResult)
        } else {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setData(uri)
            }
            context.startActivity(intent)
        }
    }

    private fun getContactId(displayName: String): List<Long> {
        val result = arrayListOf<Long>()
        val projection = arrayOf(ContactsContract.Contacts._ID)
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(displayName)

        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                result.add(id)
            }
        }

        return result
    }
}
