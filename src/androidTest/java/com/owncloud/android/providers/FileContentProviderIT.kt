/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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
package com.owncloud.android.providers

import java.lang.IllegalArgumentException
import com.owncloud.android.db.ProviderMeta
import android.content.ContentValues
import com.owncloud.android.utils.MimeTypeUtil
import org.junit.Test

@Suppress("FunctionNaming")
class FileContentProviderIT {

    companion object {
        private const val INVALID_COLUMN = "Invalid column"
        private const val FILE_LENGTH = 120
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyColumnName_Exception() {
        val sut = FileContentProvider()
        sut.verifyColumnName(INVALID_COLUMN)
    }

    @Test
    fun verifyColumnName_OK() {
        val sut = FileContentProvider()
        sut.verifyColumnName(ProviderMeta.ProviderTableMeta.FILE_NAME)
    }

    @Test
    fun verifyColumn_ContentValues_OK() {
        val sut = FileContentProvider()
        // with valid columns
        val contentValues = ContentValues()
        contentValues.put(ProviderMeta.ProviderTableMeta.FILE_CONTENT_LENGTH, FILE_LENGTH)
        contentValues.put(ProviderMeta.ProviderTableMeta.FILE_CONTENT_TYPE, MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN)
        sut.verifyColumns(contentValues)

        // empty
        sut.verifyColumns(ContentValues())
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyColumn_ContentValues_Exception() {
        val sut = FileContentProvider()
        // with valid columns
        val contentValues = ContentValues()
        contentValues.put(INVALID_COLUMN, FILE_LENGTH)
        contentValues.put(ProviderMeta.ProviderTableMeta.FILE_CONTENT_TYPE, MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN)
        sut.verifyColumns(contentValues)

        // empty
        sut.verifyColumns(ContentValues())
    }

    // @Test
    // fun verifyColumn_Uri() {
    //     val sut = FileContentProvider()
    //     sut.verifyColumnSql("path = ? AND file_owner=?")
    // }
    //
    // @Test
    // fun verifyColumn_StringArray() {
    //     val sut = FileContentProvider()
    //     val nullArray: Array<String>? = null
    //     val emptyArray = arrayOf<String>()
    //     val array = arrayOf(
    //         ProviderMeta.ProviderTableMeta.FILE_PATH,
    //         ProviderMeta.ProviderTableMeta.FILE_CONTENT_LENGTH
    //     )
    //     sut.verifyColumns(nullArray)
    //     sut.verifyColumns(emptyArray)
    //     sut.verifyColumns(array)
    // }
    // @Test
    // fun parameterizedSelection() {
    //     val sut = FileContentProvider()
    //     TestCase.assertEquals("test =?", sut.buildParameterizedSelection("test"))
    //     TestCase.assertEquals("column1 =?, column2 =?", sut.buildParameterizedSelection("column1, column2"))
    // }
}
