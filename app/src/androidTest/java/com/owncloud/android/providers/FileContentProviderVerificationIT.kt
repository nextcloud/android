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

import android.content.ContentValues
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.utils.MimeTypeUtil
import org.junit.Test
import java.lang.IllegalArgumentException

@Suppress("FunctionNaming")
class FileContentProviderVerificationIT {

    companion object {
        private const val INVALID_COLUMN = "Invalid column"
        private const val FILE_LENGTH = 120
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyColumnName_Exception() {
        FileContentProvider.VerificationUtils.verifyColumnName(INVALID_COLUMN)
    }

    @Test
    fun verifyColumnName_OK() {
        FileContentProvider.VerificationUtils.verifyColumnName(ProviderMeta.ProviderTableMeta.FILE_NAME)
    }

    @Test
    fun verifyColumn_ContentValues_OK() {
        // with valid columns
        val contentValues = ContentValues()
        contentValues.put(ProviderMeta.ProviderTableMeta.FILE_CONTENT_LENGTH, FILE_LENGTH)
        contentValues.put(ProviderMeta.ProviderTableMeta.FILE_CONTENT_TYPE, MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN)
        FileContentProvider.VerificationUtils.verifyColumns(contentValues)

        // empty
        FileContentProvider.VerificationUtils.verifyColumns(ContentValues())
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyColumn_ContentValues_invalidColumn() {
        // with invalid columns
        val contentValues = ContentValues()
        contentValues.put(INVALID_COLUMN, FILE_LENGTH)
        contentValues.put(ProviderMeta.ProviderTableMeta.FILE_CONTENT_TYPE, MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN)
        FileContentProvider.VerificationUtils.verifyColumns(contentValues)
    }

    @Test
    fun verifySortOrder_OK() {
        // null
        FileContentProvider.VerificationUtils.verifySortOrder(null)

        // empty
        FileContentProvider.VerificationUtils.verifySortOrder("")

        // valid sort
        FileContentProvider.VerificationUtils.verifySortOrder(ProviderMeta.ProviderTableMeta.FILE_DEFAULT_SORT_ORDER)
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifySortOrder_InvalidColumn() {
        // with invalid column
        FileContentProvider.VerificationUtils.verifySortOrder("$INVALID_COLUMN desc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifySortOrder_InvalidGrammar() {
        // with invalid grammar
        FileContentProvider.VerificationUtils.verifySortOrder("${ProviderMeta.ProviderTableMeta._ID} ;--foo")
    }

    @Test
    fun verifyWhere_OK() {
        FileContentProvider.VerificationUtils.verifyWhere(null)
        FileContentProvider.VerificationUtils.verifyWhere(
            "${ProviderMeta.ProviderTableMeta._ID}=? AND ${ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER}=?"
        )
        FileContentProvider.VerificationUtils.verifyWhere(
            "${ProviderMeta.ProviderTableMeta._ID} = 1" +
                " AND (1 = 1)" +
                " AND ${ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER} LIKE ?"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyWhere_InvalidColumnName() {
        FileContentProvider.VerificationUtils.verifyWhere("$INVALID_COLUMN= ?")
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyWhere_InvalidGrammar() {
        FileContentProvider.VerificationUtils.verifyWhere("1=1 -- SELECT * FROM")
    }
}
