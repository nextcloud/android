/**
 *   nextCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016  Bartosz Przybylski
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

package org.nextcloud.providers.cursors;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Build;
import android.provider.DocumentsContract.Root;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;


@TargetApi(Build.VERSION_CODES.KITKAT)
public class RootCursor extends MatrixCursor {

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS
    };

    public RootCursor(String[] projection) {
        super(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
    }

    public void addRoot(Account account, Context context) {
        final FileDataStorageManager manager =
                new FileDataStorageManager(account, context.getContentResolver());
        final OCFile mainDir = manager.getFileByPath("/");
        newRow().add(Root.COLUMN_ROOT_ID, account.name)
                .add(Root.COLUMN_DOCUMENT_ID, mainDir.getFileId())
                .add(Root.COLUMN_SUMMARY, account.name)
                .add(Root.COLUMN_TITLE, context.getString(R.string.app_name))
                .add(Root.COLUMN_ICON, R.drawable.logo)
                .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_SEARCH);

    }

}
