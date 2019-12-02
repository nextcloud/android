/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.os.AsyncTask;

import com.nextcloud.android.lib.resources.directediting.DirectEditingOpenFileRemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RichDocumentsUrlOperation;
import com.owncloud.android.ui.activity.EditorWebView;
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView;
import com.owncloud.android.ui.activity.TextEditorWebView;

import java.lang.ref.WeakReference;

public class LoadUrlTask extends AsyncTask<String, Void, String> {

    private Account account;
    private WeakReference<EditorWebView> editorWebViewWeakReference;
    private static final String TEXT = "text";

    public LoadUrlTask(EditorWebView editorWebView, Account account) {
        this.account = account;
        this.editorWebViewWeakReference = new WeakReference<>(editorWebView);
    }

    @Override
    protected String doInBackground(String... fileId) {
        final EditorWebView editorWebView = editorWebViewWeakReference.get();

        if (editorWebView == null) {
            return "";
        }

        RemoteOperationResult result;


        if (editorWebView instanceof RichDocumentsEditorWebView) {
            result = new RichDocumentsUrlOperation(fileId[0]).execute(account, editorWebViewWeakReference.get());
        } else if (editorWebView instanceof TextEditorWebView) {
            result = new DirectEditingOpenFileRemoteOperation(fileId[0], TEXT)
                .execute(account, editorWebViewWeakReference.get());
        } else {
            return "";
        }

        if (!result.isSuccess()) {
            return "";
        }

        return (String) result.getData().get(0);
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
