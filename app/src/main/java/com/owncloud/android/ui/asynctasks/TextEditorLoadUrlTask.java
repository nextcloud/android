/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.android.lib.resources.directediting.DirectEditingOpenFileRemoteOperation;
import com.nextcloud.client.account.User;
import com.nextcloud.utils.EditorUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.Editor;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.EditorWebView;

import java.lang.ref.WeakReference;

public class TextEditorLoadUrlTask extends AsyncTask<Void, Void, String> {

    private final EditorUtils editorUtils;
    private WeakReference<EditorWebView> editorWebViewWeakReference;
    private OCFile file;
    private User user;

    public TextEditorLoadUrlTask(EditorWebView editorWebView, User user, OCFile file, EditorUtils editorUtils) {
        this.user = user;
        this.editorWebViewWeakReference = new WeakReference<>(editorWebView);
        this.file = file;
        this.editorUtils = editorUtils;
    }

    @Override
    protected String doInBackground(Void... voids) {
        final EditorWebView editorWebView = editorWebViewWeakReference.get();

        if (editorWebView == null) {
            return "";
        }

        Editor editor = editorUtils.getEditor(user, file.getMimeType());

        if (editor == null) {
            return "";
        }

        RemoteOperationResult<String> result = new DirectEditingOpenFileRemoteOperation(file.getRemotePath(), editor.getId())
            .executeNextcloudClient(user, editorWebView);


        if (!result.isSuccess()) {
            return "";
        }

        return result.getResultData();
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
