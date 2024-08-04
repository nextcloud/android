/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021-2022 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.util;

import android.content.Context;
import android.content.res.Resources;

import com.nextcloud.client.account.MockUser;
import com.nextcloud.client.account.User;
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
        User user = new MockUser("name", ACCOUNT_TYPE);
        Context context = MainApp.getAppContext();

        String errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
            new RemoteOperationResult(RemoteOperationResult.ResultCode.FORBIDDEN),
            new RemoveFileOperation(new OCFile(PATH_TO_DELETE),
                                    false,
                                    user,
                                    false,
                                    context,
                                    new FileDataStorageManager(user, context.getContentResolver())),
            resources
                                                                      );

        assertEquals(EXPECTED_ERROR_MESSAGE, errorMessage);
    }
}
