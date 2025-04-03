/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import androidx.work.WorkManager
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.test.RetryTestRule
import com.nextcloud.utils.extensions.StringConstants
import com.owncloud.android.AbstractIT
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.DownloadFileOperation
import ezvcard.Ezvcard
import ezvcard.VCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

class ContactsBackupIT : AbstractOnServerIT() {
    private val workManager = WorkManager.getInstance(targetContext)
    private val preferences: AppPreferences = AppPreferencesImpl.fromContext(targetContext)
    private val backgroundJobManager = BackgroundJobManagerImpl(workManager, ClockImpl(), preferences)

    @get:Rule
    val writeContactsRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    @get:Rule
    val readContactsRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

    @get:Rule
    val retryTestRule = RetryTestRule() // flaky test

    private val vcard: String = "vcard.vcf"

    @Test
    fun importExport() {
        val intArray = intArrayOf(0)
        val selectedContactsFile = File(targetContext.cacheDir, "hashset_cache.txt")
        selectedContactsFile.writeText(intArray.joinToString(StringConstants.DELIMITER))

        // import file to local contacts
        backgroundJobManager.startImmediateContactsImport(
            null,
            null,
            getFile(vcard).absolutePath,
            selectedContactsFile.absolutePath
        )
        longSleep()

        // export contact
        backgroundJobManager.startImmediateContactsBackup(user)
        longSleep()

        val folderPath: String = targetContext.resources.getString(R.string.contacts_backup_folder) +
            OCFile.PATH_SEPARATOR

        refreshFolder("/")
        longSleep()
        longSleep()

        refreshFolder(folderPath)
        longSleep()
        longSleep()

        if (folderPath.isEmpty()) {
            fail("folderPath cannot be empty")
        }

        val folder = fileDataStorageManager.getFileByDecryptedRemotePath(folderPath)
        if (folder == null) {
            fail("folder cannot be null")
        }

        val ocFile = storageManager.getFolderContent(folder, false).firstOrNull()
        if (ocFile == null) {
            fail("ocFile cannot be null")
        }

        if (ocFile?.storagePath == null) {
            fail("ocFile.storagePath cannot be null")
        }

        assertTrue(DownloadFileOperation(user, ocFile, AbstractIT.targetContext).execute(client).isSuccess)

        val file = ocFile?.storagePath?.let { File(it) }
        if (file == null) {
            fail("file cannot be null")
        }

        val vcardInputStream = BufferedInputStream(FileInputStream(getFile(vcard)))
        val backupFileInputStream = BufferedInputStream(FileInputStream(file))

        // verify same
        val originalCards: ArrayList<VCard> = ArrayList()
        originalCards.addAll(Ezvcard.parse(vcardInputStream).all())

        val backupCards: ArrayList<VCard> = ArrayList()
        backupCards.addAll(Ezvcard.parse(backupFileInputStream).all())

        assertEquals(originalCards.size, backupCards.size)

        val originalCardFormattedName = originalCards.firstOrNull()?.formattedName
        if (originalCardFormattedName == null) {
            fail("originalCardFormattedName cannot be null")
        }

        val backupCardFormattedName = backupCards.firstOrNull()?.formattedName
        if (backupCardFormattedName == null) {
            fail("backupCardFormattedName cannot be null")
        }

        assertEquals(originalCardFormattedName.toString(), backupCardFormattedName.toString())
    }
}
