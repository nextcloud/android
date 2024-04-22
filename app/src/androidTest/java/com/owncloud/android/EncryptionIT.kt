/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android

import com.owncloud.android.datamodel.OCFile
import java.security.SecureRandom

open class EncryptionIT : AbstractIT() {

    fun testFolder(): OCFile {
        val rootPath = "/"
        val folderPath = "/TestFolder/"

        OCFile(rootPath).apply {
            storageManager.saveFile(this)
        }

        return OCFile(folderPath).apply {
            decryptedRemotePath = folderPath
            isEncrypted = true
            fileLength = SecureRandom().nextLong()
            setFolder()
            parentId = storageManager.getFileByDecryptedRemotePath(rootPath)!!.fileId
            storageManager.saveFile(this)
        }
    }
}
