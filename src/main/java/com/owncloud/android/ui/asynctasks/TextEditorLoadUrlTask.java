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
import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.Editor;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.EditorWebView;

import java.lang.ref.WeakReference;

public class TextEditorLoadUrlTask extends AsyncTask<Void, Void, String> {

    private Account account;
    private WeakReference<EditorWebView> editorWebViewWeakReference;
    private OCFile file;
    private User user;

    public TextEditorLoadUrlTask(EditorWebView editorWebView, User user, OCFile file) {
        this.user = user;
        this.account = user.toPlatformAccount();
        this.editorWebViewWeakReference = new WeakReference<>(editorWebView);
        this.file = file;
    }

    @Override
    protected String doInBackground(Void... voids) {
        final EditorWebView editorWebView = editorWebViewWeakReference.get();

        if (editorWebView == null) {
            return "";
        }

        Editor editor = FileMenuFilter.getEditor(editorWebView.getContentResolver(), user, file.getMimeType());

        if (editor == null) {
            return "";
        }

        RemoteOperationResult result = new DirectEditingOpenFileRemoteOperation(file.getRemotePath(), editor.id)
            .execute(account, editorWebViewWeakReference.get());


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
