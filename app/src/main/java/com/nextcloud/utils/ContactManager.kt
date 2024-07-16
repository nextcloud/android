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
import com.nextcloud.utils.extensions.email
import com.nextcloud.utils.extensions.phoneNumber
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
            val contactIds = getContactIds(displayName)
            if (contactIds.size > 1) {
                getContactId(searchResult, contactIds)
            } else {
                contactIds.firstOrNull()
            }
        } else {
            null
        }

        if (contactId == null) {
            val messageId = if (havePermission) {
                R.string.unified_search_fragment_contact_not_found
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

    private fun getContactId(searchResult: SearchResultEntry, contactIds: List<Long>): Long? {
        val email = searchResult.email()
        val phoneNumber = searchResult.phoneNumber()

        for (contactId in contactIds) {
            val targetEmail = getEmailById(contactId)
            val targetPhoneNumber = getPhoneNumberById(contactId)
            if (targetEmail == email && targetPhoneNumber == phoneNumber) {
                return contactId
            }
        }

        return null
    }

    private fun getEmailById(contactId: Long): String? {
        var email: String? = null
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                if (emailIndex != -1) {
                    email = it.getString(emailIndex)
                }
            }
        }
        return email
    }

    private fun getPhoneNumberById(contactId: Long): String? {
        var phoneNumber: String? = null
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (phoneIndex != -1) {
                    phoneNumber = it.getString(phoneIndex)
                }
            }
        }
        return phoneNumber
    }

    private fun getContactIds(displayName: String): List<Long> {
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
