/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface

class ContactManager(private val context: Context) {

    fun openContact(searchResult: SearchResultEntry, listInterface: UnifiedSearchListInterface) {
        val contactIds = getContactIds(searchResult.title)

        if (contactIds.isEmpty()) {
            context.showToast(R.string.unified_search_fragment_contact_cannot_be_found_on_device)
            listInterface.onSearchResultClicked(searchResult)
        } else {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactIds.first().toString())
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setData(uri)
            }
            context.startActivity(intent)
        }
    }

    private fun getContactIds(contactName: String): List<Long> {
        val result = arrayListOf<Long>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(contactName)

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
