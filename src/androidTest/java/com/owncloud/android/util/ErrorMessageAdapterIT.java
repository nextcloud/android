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


package com.owncloud.android.util;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.utils.ErrorMessageAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ErrorMessageAdapterIT {
    private final static String PATH_TO_DELETE = "/path/to/a.file";
    private final static String EXPECTED_ERROR_MESSAGE = "You are not permitted to delete this file";
    private final static String ACCOUNT_TYPE = "nextcloud";

    @Test
    public void getErrorCauseMessageForForbiddenRemoval() {
        Resources resources = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources();
        Account account = new Account("name", ACCOUNT_TYPE);
        Context context = MainApp.getAppContext();

        String errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
            new RemoteOperationResult(RemoteOperationResult.ResultCode.FORBIDDEN),
            new RemoveFileOperation(new OCFile(PATH_TO_DELETE),
                                    false,
                                    account,
                                    false,
                                    context,
                                    new FileDataStorageManager(account, context.getContentResolver())),
            resources
                                                                      );

        assertEquals(EXPECTED_ERROR_MESSAGE, errorMessage);
    }
}
