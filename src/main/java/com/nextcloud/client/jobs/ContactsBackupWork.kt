/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.jobs

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.IBinder
import android.provider.ContactsContract
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.files.downloader.PostUploadAction
import com.nextcloud.client.files.downloader.TransferManagerConnection
import com.nextcloud.client.files.downloader.UploadRequest
import com.nextcloud.client.files.downloader.UploadTrigger
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.services.OperationsService
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import ezvcard.Ezvcard
import ezvcard.VCardVersion
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Calendar

@Suppress("LongParameterList") // legacy code
class ContactsBackupWork(
    appContext: Context,
    params: WorkerParameters,
    private val resources: Resources,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val contentResolver: ContentResolver,
    private val accountManager: UserAccountManager
) : Worker(appContext, params) {

    companion object {
        val TAG = ContactsBackupWork::class.java.simpleName
        const val ACCOUNT = "account"
        const val FORCE = "force"
        const val JOB_INTERVAL_MS: Long = 24 * 60 * 60 * 1000
        const val BUFFER_SIZE = 1024
    }

    private var operationsServiceConnection: OperationsServiceConnection? = null
    private var operationsServiceBinder: OperationsServiceBinder? = null

    @Suppress("ReturnCount") // pre-existing issue
    override fun doWork(): Result {
        val accountName = inputData.getString(ACCOUNT) ?: ""
        if (TextUtils.isEmpty(accountName)) { // no account provided
            return Result.failure()
        }
        val optionalUser = accountManager.getUser(accountName)
        if (!optionalUser.isPresent) {
            return Result.failure()
        }
        val user = optionalUser.get()
        val lastExecution = arbitraryDataProvider.getLongValue(
            user,
            ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP
        )
        val force = inputData.getBoolean(FORCE, false)
        if (force || lastExecution + JOB_INTERVAL_MS < Calendar.getInstance().timeInMillis) {
            Log_OC.d(TAG, "start contacts backup job")
            val backupFolder: String = resources.getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR
            val daysToExpire: Int = applicationContext.getResources().getInteger(R.integer.contacts_backup_expire)
            backupContact(user, backupFolder)
            // bind to Operations Service
            operationsServiceConnection = OperationsServiceConnection(
                this,
                daysToExpire,
                backupFolder,
                user
            )
            applicationContext.bindService(
                Intent(applicationContext, OperationsService::class.java),
                operationsServiceConnection as OperationsServiceConnection,
                OperationsService.BIND_AUTO_CREATE
            )
            // store execution date
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP,
                Calendar.getInstance().timeInMillis
            )
        } else {
            Log_OC.d(TAG, "last execution less than 24h ago")
        }
        return Result.success()
    }

    private fun backupContact(user: User, backupFolder: String) {
        val vCard = ArrayList<String>()
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            for (i in 0 until cursor.count) {
                vCard.add(getContactFromCursor(cursor))
                cursor.moveToNext()
            }
        }
        val filename = DateFormat.format("yyyy-MM-dd_HH-mm-ss", Calendar.getInstance()).toString() + ".vcf"
        Log_OC.d(TAG, "Storing: $filename")
        val file = File(applicationContext.getCacheDir(), filename)
        var fw: FileWriter? = null
        try {
            fw = FileWriter(file)
            for (card in vCard) {
                fw.write(card)
            }
        } catch (e: IOException) {
            Log_OC.d(TAG, "Error ", e)
        } finally {
            cursor?.close()
            if (fw != null) {
                try {
                    fw.close()
                } catch (e: IOException) {
                    Log_OC.d(TAG, "Error closing file writer ", e)
                }
            }
        }

        val request = UploadRequest.Builder(user, file.absolutePath, backupFolder + file.name)
            .setFileSize(file.length())
            .setNameConflicPolicy(NameCollisionPolicy.RENAME)
            .setCreateRemoteFolder(true)
            .setTrigger(UploadTrigger.USER)
            .setPostAction(PostUploadAction.MOVE_TO_APP)
            .setRequireWifi(false)
            .setRequireCharging(false)
            .build()

        val connection = TransferManagerConnection(applicationContext, user)
        connection.enqueue(request)
    }

    private fun expireFiles(daysToExpire: Int, backupFolderString: String, user: User) { // -1 disables expiration
        if (daysToExpire > -1) {
            val storageManager = FileDataStorageManager(
                user.toPlatformAccount(),
                applicationContext.getContentResolver()
            )
            val backupFolder: OCFile = storageManager.getFileByPath(backupFolderString)
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysToExpire)
            val timestampToExpire = cal.timeInMillis
            if (backupFolder != null) {
                Log_OC.d(TAG, "expire: " + daysToExpire + " " + backupFolder.fileName)
            }
            val backups: List<OCFile> = storageManager.getFolderContent(backupFolder, false)
            for (backup in backups) {
                if (timestampToExpire > backup.modificationTimestamp) {
                    Log_OC.d(TAG, "delete " + backup.remotePath)
                    // delete backups
                    val service = Intent(applicationContext, OperationsService::class.java)
                    service.action = OperationsService.ACTION_REMOVE
                    service.putExtra(OperationsService.EXTRA_ACCOUNT, user.toPlatformAccount())
                    service.putExtra(OperationsService.EXTRA_REMOTE_PATH, backup.remotePath)
                    service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, false)
                    operationsServiceBinder!!.queueNewOperation(service)
                }
            }
        }
        operationsServiceConnection?.let {
            applicationContext.unbindService(it)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun getContactFromCursor(cursor: Cursor): String {
        val lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
        var vCard = ""
        var inputStream: InputStream? = null
        var inputStreamReader: InputStreamReader? = null
        try {
            inputStream = applicationContext.getContentResolver().openInputStream(uri)
            val buffer = CharArray(BUFFER_SIZE)
            val stringBuilder = StringBuilder()
            if (inputStream != null) {
                inputStreamReader = InputStreamReader(inputStream)
                while (true) {
                    val byteCount = inputStreamReader.read(buffer, 0, buffer.size)
                    if (byteCount > 0) {
                        stringBuilder.append(buffer, 0, byteCount)
                    } else {
                        break
                    }
                }
            }
            vCard = stringBuilder.toString()
            // bump to vCard 3.0 format (min version supported by server) since Android OS exports to 2.1
            return Ezvcard.write(Ezvcard.parse(vCard).all()).version(VCardVersion.V3_0).go()
        } catch (e: IOException) {
            Log_OC.d(TAG, e.message)
        } finally {
            try {
                inputStream?.close()
                inputStreamReader?.close()
            } catch (e: IOException) {
                Log_OC.e(TAG, "failed to close stream")
            }
        }
        return vCard
    }

    /**
     * Implements callback methods for service binding.
     */
    private class OperationsServiceConnection internal constructor(
        private val worker: ContactsBackupWork,
        private val daysToExpire: Int,
        private val backupFolder: String,
        private val user: User
    ) : ServiceConnection {
        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            Log_OC.d(TAG, "service connected")
            if (component == ComponentName(worker.applicationContext, OperationsService::class.java)) {
                worker.operationsServiceBinder = service as OperationsServiceBinder
                worker.expireFiles(daysToExpire, backupFolder, user)
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            Log_OC.d(TAG, "service disconnected")
            if (component == ComponentName(worker.applicationContext, OperationsService::class.java)) {
                worker.operationsServiceBinder = null
            }
        }
    }
}
