/*
 *
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
package com.owncloud.android.datamodel

import com.owncloud.android.db.ProviderMeta
import org.junit.Assert
import org.junit.Test

class FileDataStorageManagerContentProviderClientIT : FileDataStorageManagerIT() {
    override fun before() {
        sut = FileDataStorageManager(
            user,
            targetContext
                .contentResolver
                .acquireContentProviderClient(ProviderMeta.ProviderTableMeta.CONTENT_URI)
        )
        super.before()
    }

    @Test
    fun saveFile() {
        val path = "/1.txt"
        val file = OCFile(path)
        file.remoteId = "00000008ocjycgrudn78"

        // TODO check via reflection that every parameter is set
        file.fileLength = 1024000
        file.modificationTimestamp = 1582019340
        sut.saveNewFile(file)
        val read = sut.getFileByPath(path)
        Assert.assertNotNull(read)
        Assert.assertEquals(file.remotePath, read.remotePath)
    }
}