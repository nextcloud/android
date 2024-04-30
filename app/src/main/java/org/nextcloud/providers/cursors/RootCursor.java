/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Jens Mueller <tschenser@gmx.de>
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package org.nextcloud.providers.cursors;

import android.content.Context;
import android.database.MatrixCursor;
import android.provider.DocumentsContract.Root;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.providers.DocumentsStorageProvider;

public class RootCursor extends MatrixCursor {

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS
    };

    public RootCursor(String... projection) {
        super(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
    }

    public void addRoot(DocumentsStorageProvider.Document document, Context context) {
        User user = document.getUser();

        int rootFlags = Root.FLAG_SUPPORTS_CREATE
                      | Root.FLAG_SUPPORTS_RECENTS
                      | Root.FLAG_SUPPORTS_SEARCH
                      | Root.FLAG_SUPPORTS_IS_CHILD;

        newRow().add(Root.COLUMN_ROOT_ID, user.getAccountName())
            .add(Root.COLUMN_DOCUMENT_ID, document.getDocumentId())
            .add(Root.COLUMN_SUMMARY, user.getAccountName())
            .add(Root.COLUMN_TITLE, context.getString(R.string.app_name))
            .add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            .add(Root.COLUMN_FLAGS, rootFlags);
    }
}
