/**
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


package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.res.Resources;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Local unit test, to be run out of Android emulator or device.
 *
 * At the moment, it's a sample to validate the automatic test environment, in the scope of local unit tests with
 * mock Android dependencies.
 *
 * Don't take it as an example of completeness.
 *
 * See http://developer.android.com/intl/es/training/testing/unit-testing/local-unit-tests.html .
 */
@RunWith(MockitoJUnitRunner.class)
public class ErrorMessageAdapterUnitTest {

    private final static String MOCK_FORBIDDEN_PERMISSIONS = "You do not have permission %s";
    private final static String MOCK_TO_DELETE = "to delete this file";
    private final static String PATH_TO_DELETE = "/path/to/a.file";
    private final static String EXPECTED_ERROR_MESSAGE = "You do not have permission to delete this file";
    private final static String ACCOUNT_TYPE = "nextcloud";

    @Mock
    Resources mMockResources;

    @Test
    public void getErrorCauseMessageForForbiddenRemoval() {
        // Given a mocked set of resources passed to the object under test...
        when(mMockResources.getString(R.string.forbidden_permissions))
            .thenReturn(MOCK_FORBIDDEN_PERMISSIONS);
        when(mMockResources.getString(R.string.forbidden_permissions_delete))
            .thenReturn(MOCK_TO_DELETE);

        Account account = new Account("name", ACCOUNT_TYPE);

        // ... when method under test is called ...
        String errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
            new RemoteOperationResult(RemoteOperationResult.ResultCode.FORBIDDEN),
                new RemoveFileOperation(PATH_TO_DELETE, false, account, MainApp.getAppContext()),
            mMockResources
        );

        // ... then the result should be the expected one.
        assertThat(errorMessage, is(EXPECTED_ERROR_MESSAGE));

    }
}
