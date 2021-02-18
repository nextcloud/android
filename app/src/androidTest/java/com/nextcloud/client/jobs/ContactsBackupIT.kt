/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

package com.nextcloud.client.jobs

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import androidx.work.WorkManager
import com.nextcloud.client.core.ClockImpl
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
import java.io.File

class ContactsBackupIT : AbstractOnServerIT() {
    val workmanager = WorkManager.getInstance(targetContext)
    private val backgroundJobManager = BackgroundJobManagerImpl(workmanager, ClockImpl())

    @get:Rule
    val writeContactsRule = GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    @get:Rule
    val readContactsRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

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

        assertTrue(DownloadFileOperation(account, backupOCFile, AbstractIT.targetContext).execute(client).isSuccess)

        val backupFile = File(backupOCFile.storagePath)

        // verify same
        val originalCards: ArrayList<VCard> = ArrayList()
        originalCards.addAll(Ezvcard.parse(getFile(vcard)).all())

        val backupCards: ArrayList<VCard> = ArrayList()
        backupCards.addAll(Ezvcard.parse(backupFile).all())

        assertEquals(originalCards.size, backupCards.size)
        assertEquals(originalCards[0].formattedName.toString(), backupCards[0].formattedName.toString())
    }
}
