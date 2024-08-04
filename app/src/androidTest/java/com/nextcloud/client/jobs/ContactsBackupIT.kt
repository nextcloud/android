/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import androidx.work.WorkManager
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.DownloadFileOperation
import ezvcard.Ezvcard
import ezvcard.VCard
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

class ContactsBackupIT : AbstractOnServerIT() {
    val workmanager = WorkManager.getInstance(targetContext)
    val preferences = AppPreferencesImpl.fromContext(targetContext)
    private val backgroundJobManager = BackgroundJobManagerImpl(workmanager, ClockImpl(), preferences)

    @get:Rule
    val writeContactsRule = GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    @get:Rule
    val readContactsRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

    @get:Rule
    val retryTestRule = RetryTestRule() // flaky test

    private val vcard: String = "vcard.vcf"

    @Test
    fun importExport() {
        val intArray = IntArray(1)
        intArray[0] = 0

        // import file to local contacts
        backgroundJobManager.startImmediateContactsImport(null, null, getFile(vcard).absolutePath, intArray)

        shortSleep()

        // export contact
        backgroundJobManager.startImmediateContactsBackup(user)

        longSleep()

        val backupFolder: String = targetContext.resources.getString(R.string.contacts_backup_folder) +
            OCFile.PATH_SEPARATOR

        refreshFolder("/")
        longSleep()

        refreshFolder(backupFolder)
        longSleep()

        val backupOCFile = storageManager.getFolderContent(
            storageManager.getFileByDecryptedRemotePath(backupFolder),
            false
        )[0]

        assertTrue(DownloadFileOperation(user, backupOCFile, AbstractIT.targetContext).execute(client).isSuccess)

        val backupFile = File(backupOCFile.storagePath)
        val vcardInputStream = BufferedInputStream(FileInputStream(getFile(vcard)))
        val backupFileInputStream = BufferedInputStream(FileInputStream(backupFile))

        // verify same
        val originalCards: ArrayList<VCard> = ArrayList()
        originalCards.addAll(Ezvcard.parse(vcardInputStream).all())

        val backupCards: ArrayList<VCard> = ArrayList()
        backupCards.addAll(Ezvcard.parse(backupFileInputStream).all())

        assertEquals(originalCards.size, backupCards.size)
        assertEquals(originalCards[0].formattedName.toString(), backupCards[0].formattedName.toString())
    }
}
