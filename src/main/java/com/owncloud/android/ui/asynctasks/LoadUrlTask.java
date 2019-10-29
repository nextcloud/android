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
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RichDocumentsUrlOperation;
import com.owncloud.android.ui.activity.RichDocumentsWebView;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.lang.ref.WeakReference;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class LoadUrlTask extends AsyncTask<String, Void, String> {

    private Account account;
    private WeakReference<RichDocumentsWebView> richDocumentsWebViewWeakReference;

    public LoadUrlTask(RichDocumentsWebView richDocumentsWebView, Account account) {
        this.account = account;
        this.richDocumentsWebViewWeakReference = new WeakReference<>(richDocumentsWebView);
    }

    @Override
    protected String doInBackground(String... fileId) {
        if (richDocumentsWebViewWeakReference.get() == null) {
            return "";
        }
        RichDocumentsUrlOperation richDocumentsUrlOperation = new RichDocumentsUrlOperation(fileId[0]);
        RemoteOperationResult result = richDocumentsUrlOperation.execute(account,
                                                                         richDocumentsWebViewWeakReference.get());

        if (!result.isSuccess()) {
            return "";
        }

        return (String) result.getData().get(0);
    }

    @Override
    protected void onPostExecute(String url) {
        RichDocumentsWebView richDocumentsWebView = richDocumentsWebViewWeakReference.get();

        if (richDocumentsWebView == null) {
            return;
        }

        if (!url.isEmpty()) {
            richDocumentsWebView.getWebview().loadUrl(url);

            new Handler().postDelayed(() -> {
                if (richDocumentsWebView.getWebview().getVisibility() != View.VISIBLE) {
                    Snackbar snackbar = DisplayUtils.createSnackbar(richDocumentsWebView.findViewById(android.R.id.content),
                                                                    R.string.timeout_richDocuments, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.fallback_weblogin_back, v -> richDocumentsWebView.closeView());

                    ThemeUtils.colorSnackbar(richDocumentsWebView.getApplicationContext(),snackbar);
                    richDocumentsWebView.setLoadingSnackbar(snackbar);
                    snackbar.show();
                }
            }, 10 * 1000);
        } else {
            Toast.makeText(richDocumentsWebView.getApplicationContext(),
                           R.string.richdocuments_failed_to_load_document, Toast.LENGTH_LONG).show();
            richDocumentsWebView.finish();
        }
    }
}
