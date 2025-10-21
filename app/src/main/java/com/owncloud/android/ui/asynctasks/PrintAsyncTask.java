/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView;
import com.owncloud.android.ui.adapter.PrintAdapter;
import com.owncloud.android.utils.DisplayUtils;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.Objects;

import static android.content.Context.PRINT_SERVICE;

public class PrintAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = PrintAsyncTask.class.getSimpleName();
    private static final String JOB_NAME = "Document";

    private File file;
    private String url;
    private WeakReference<RichDocumentsEditorWebView> richDocumentsWebViewWeakReference;

    public PrintAsyncTask(File file, String url, WeakReference<RichDocumentsEditorWebView> richDocumentsWebViewWeakReference) {
        this.file = file;
        this.url = url;
        this.richDocumentsWebViewWeakReference = richDocumentsWebViewWeakReference;
    }

    @Override
    protected void onPreExecute() {
        richDocumentsWebViewWeakReference.get().runOnUiThread(
            () -> richDocumentsWebViewWeakReference.get().showLoadingDialog(
                richDocumentsWebViewWeakReference.get().getString(R.string.common_loading)));

        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        if (file == null) return Boolean.FALSE;

        HttpClient client = new HttpClient();
        GetMethod getMethod = null;

        try {
            getMethod = new GetMethod(url);
            int status = client.executeMethod(getMethod);
            if (status != HttpStatus.SC_OK) return Boolean.FALSE;

            if (file.exists() && !file.delete()) return Boolean.FALSE;

            File parentFile = file.getParentFile();
            if (parentFile == null) return Boolean.FALSE;

            Files.createDirectories(parentFile.toPath());
            if (!parentFile.exists()) {
                Log_OC.d(TAG, parentFile.getAbsolutePath() + " does not exist");
                return Boolean.FALSE;
            }

            if (!file.createNewFile()) {
                Log_OC.d(TAG, file.getAbsolutePath() + " could not be created");
                return Boolean.FALSE;
            }

            Header contentLengthHeader = getMethod.getResponseHeader("Content-Length");
            long totalToTransfer = 0;
            if (contentLengthHeader != null && !contentLengthHeader.getValue().isEmpty()) {
                totalToTransfer = Long.parseLong(contentLengthHeader.getValue());
            }

            try (
                BufferedInputStream bis = new BufferedInputStream(getMethod.getResponseBodyAsStream());
                FileOutputStream fos = new FileOutputStream(file)
            ) {
                byte[] buffer = new byte[4096];
                long transferred = 0;
                int read;

                while ((read = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    transferred += read;
                }

                if (totalToTransfer > 0 && transferred != totalToTransfer) {
                    Log_OC.d(TAG, "Transferred bytes (" + transferred +
                        ") != expected (" + totalToTransfer + ")");
                    return Boolean.FALSE;
                }
            }

            return Boolean.TRUE;
        } catch (Exception e) {
            Log_OC.e(TAG, "Error downloading file", e);
            return Boolean.FALSE;
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        RichDocumentsEditorWebView richDocumentsWebView = richDocumentsWebViewWeakReference.get();
        richDocumentsWebView.dismissLoadingDialog();

        PrintManager printManager = (PrintManager) richDocumentsWebView.getSystemService(PRINT_SERVICE);

        if (!result || printManager == null) {
            DisplayUtils.showSnackMessage(richDocumentsWebView,
                                          richDocumentsWebView.getString(R.string.failed_to_print));

            return;
        }

        PrintDocumentAdapter printAdapter = new PrintAdapter(file.getAbsolutePath());

        printManager.print(JOB_NAME, printAdapter, new PrintAttributes.Builder().build());
    }
}
