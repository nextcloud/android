/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
import com.owncloud.android.utils.ErrorMessageAdapter
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorMessageAdapterIT {
    @get:Test
    val errorCauseMessageForForbiddenRemoval: Unit
        get() {
            val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
            val user: User = MockUser("name", ACCOUNT_TYPE)
            val context = MainApp.getAppContext()
            val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
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