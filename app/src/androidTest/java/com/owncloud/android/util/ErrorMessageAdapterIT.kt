/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021-2022 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.MockUser
import com.nextcloud.client.account.User
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.utils.ErrorMessageAdapter.getErrorCauseMessage
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorMessageAdapterIT {
    @Test
    fun getErrorCauseMessageForForbiddenRemoval() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val user: User = MockUser("name", ACCOUNT_TYPE)
        val context = MainApp.getAppContext()

        val errorMessage = getErrorCauseMessage(
            RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.FORBIDDEN),
            RemoveFileOperation(
                OCFile(PATH_TO_DELETE),
                false,
                user,
                false,
                context,
                FileDataStorageManager(user, context.contentResolver)
            ),
            resources
        )

        TestCase.assertEquals(EXPECTED_ERROR_MESSAGE, errorMessage)
    }

    companion object {
        private const val PATH_TO_DELETE = "/path/to/a.file"
        private const val EXPECTED_ERROR_MESSAGE = "You are not permitted to delete this file"
        private const val ACCOUNT_TYPE = "nextcloud"
    }
}
