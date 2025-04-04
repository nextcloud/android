/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RichDocumentsUrlOperation;
import com.owncloud.android.ui.activity.EditorWebView;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

public class RichDocumentsLoadUrlTask extends AsyncTask<Void, Void, String> {

    private final User user;
    private final WeakReference<EditorWebView> editorWebViewWeakReference;
    private final OCFile file;
    private final FileDataStorageManager fileDataStorageManager;

    public RichDocumentsLoadUrlTask(EditorWebView editorWebView, User user, OCFile file, FileDataStorageManager fileDataStorageManager) {
        this.user = user;
        this.editorWebViewWeakReference = new WeakReference<>(editorWebView);
        this.file = file;
        this.fileDataStorageManager = fileDataStorageManager;
    }

    @Override
    protected String doInBackground(Void... voids) {
        final EditorWebView editorWebView = editorWebViewWeakReference.get();

        if (editorWebView == null) {
            return "";
        }

        final var result = new RichDocumentsUrlOperation(file.getLocalId()).execute(user, editorWebView);

        if (!result.isSuccess()) {
            return "";
        }

        final var url = (String) result.getData().get(0);
        file.setRichDocumentURL(url);

        // FIXME: Update file entity
        fileDataStorageManager.saveFile(file);

        return url;
    }

    @Override
    protected void onPostExecute(String url) {
        EditorWebView editorWebView = editorWebViewWeakReference.get();

        if (editorWebView == null) {
            return;
        }

        editorWebView.onUrlLoaded(url);
    }
}
