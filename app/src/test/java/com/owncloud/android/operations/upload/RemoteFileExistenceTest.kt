/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import com.nextcloud.utils.extensions.isNonRetryable
import com.owncloud.android.db.UploadResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoteFileExistenceTest {

    private val noHeaders: Array<Header>? = null

    @Test
    fun unauthorizedExistenceCheckIsNotReportedAsExistingFile() {
        val result = RemoteOperationResult<Any?>(true, HttpStatus.SC_UNAUTHORIZED, "Unauthorized", noHeaders)

        assertEquals(RemoteFileExistence.UNAUTHORIZED, RemoteFileExistence.fromExistenceCheck(result))
    }

    @Test
    fun successfulExistenceCheckMeansFileExists() {
        val result = RemoteOperationResult<Any?>(true, HttpStatus.SC_OK, "OK", noHeaders)

        assertEquals(RemoteFileExistence.EXISTS, RemoteFileExistence.fromExistenceCheck(result))
    }

    @Test
    fun notFoundExistenceCheckMeansFileDoesNotExist() {
        val result = RemoteOperationResult<Any?>(false, HttpStatus.SC_NOT_FOUND, "Not Found", noHeaders)

        assertEquals(RemoteFileExistence.DOES_NOT_EXIST, RemoteFileExistence.fromExistenceCheck(result))
    }

    @Test
    fun credentialErrorStaysRetryable() {
        val uploadResult = UploadResult.fromOperationResult(RemoteOperationResult<Any?>(ResultCode.UNAUTHORIZED))

        assertEquals(UploadResult.CREDENTIAL_ERROR, uploadResult)
        assertFalse(uploadResult.isNonRetryable())
    }
}
