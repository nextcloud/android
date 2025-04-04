/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.logger.Logger
import com.nextcloud.utils.extensions.toIntArray
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment
import com.owncloud.android.ui.fragment.contactsbackup.VCardComparator
import ezvcard.Ezvcard
import ezvcard.VCard
import org.apache.commons.io.FileUtils
import third_parties.ezvcard_android.ContactOperations
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Collections
import java.util.TreeMap

class ContactsImportWork(
    appContext: Context,
    params: WorkerParameters,
    private val logger: Logger,
    private val contentResolver: ContentResolver
) : Worker(appContext, params) {

    companion object {
        const val TAG = "ContactsImportWork"
        const val ACCOUNT_TYPE = "account_type"
        const val ACCOUNT_NAME = "account_name"
        const val VCARD_FILE_PATH = "vcard_file_path"
        const val SELECTED_CONTACTS_FILE_PATH = "selected_contacts_file_path"
    }

    @Suppress("ComplexMethod", "NestedBlockDepth", "LongMethod", "ReturnCount") // legacy code
    override fun doWork(): Result {
        val vCardFilePath = inputData.getString(VCARD_FILE_PATH) ?: ""
        val contactsAccountName = inputData.getString(ACCOUNT_NAME)
        val contactsAccountType = inputData.getString(ACCOUNT_TYPE)
        val selectedContactsFilePath = inputData.getString(SELECTED_CONTACTS_FILE_PATH)
        if (selectedContactsFilePath == null) {
            Log_OC.d(TAG, "selectedContactsFilePath is null")
            return Result.failure()
        }

        val selectedContactsFile = File(selectedContactsFilePath)
        if (!selectedContactsFile.exists()) {
            Log_OC.d(TAG, "selectedContactsFile not exists")
            return Result.failure()
        }

        val selectedContactsIndices = readCheckedContractsFromFile(selectedContactsFile)

        val inputStream = BufferedInputStream(FileInputStream(vCardFilePath))
        val vCards = ArrayList<VCard>()

        var cursor: Cursor? = null
        @Suppress("TooGenericExceptionCaught") // legacy code
        try {
            val operations = ContactOperations(applicationContext, contactsAccountName, contactsAccountType)
            vCards.addAll(Ezvcard.parse(inputStream).all())
            Collections.sort(
                vCards,
                VCardComparator()
            )
            cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            val ownContactMap = TreeMap<VCard, Long?>(VCardComparator())
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                for (i in 0 until cursor.count) {
                    val vCard = getContactFromCursor(cursor)
                    if (vCard != null) {
                        ownContactMap[vCard] = cursor.getLong(cursor.getColumnIndexOrThrow("NAME_RAW_CONTACT_ID"))
                    }
                    cursor.moveToNext()
                }
            }
            for (contactIndex in selectedContactsIndices) {
                val vCard = vCards[contactIndex]
                if (BackupListFragment.getDisplayName(vCard).isEmpty()) {
                    if (!ownContactMap.containsKey(vCard)) {
                        operations.insertContact(vCard)
                    } else {
                        operations.updateContact(vCard, ownContactMap[vCard])
                    }
                } else {
                    operations.insertContact(vCard) // Insert All the contacts without name
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "${e.message}", e)
        } finally {
            cursor?.close()
        }

        try {
            inputStream.close()
        } catch (e: IOException) {
            logger.e(TAG, "Error closing vCard stream", e)
        }

        Log_OC.d(TAG, "ContractsImportWork successfully completed")
        selectedContactsFile.delete()
        return Result.success()
    }

    @Suppress("TooGenericExceptionCaught")
    fun readCheckedContractsFromFile(file: File): IntArray {
        return try {
            val fileData = FileUtils.readFileToByteArray(file)
            fileData.toIntArray()
        } catch (e: Exception) {
            Log_OC.e(TAG, "Exception readCheckedContractsFromFile: $e")
            intArrayOf()
        }
    }

    private fun getContactFromCursor(cursor: Cursor): VCard? {
        val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
        var vCard: VCard? = null
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val vCardList = ArrayList<VCard>()
                vCardList.addAll(Ezvcard.parse(inputStream).all())
                if (vCardList.size > 0) {
                    vCard = vCardList[0]
                }
            }
        } catch (e: IOException) {
            logger.d(TAG, "${e.message}")
        }
        return vCard
    }
}
