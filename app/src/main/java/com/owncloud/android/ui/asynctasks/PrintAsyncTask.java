/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        HttpClient client = new HttpClient();
        GetMethod getMethod = null;

        FileOutputStream fos;
        try {
            getMethod = new GetMethod(url);
            int status = client.executeMethod(getMethod);
            if (status == HttpStatus.SC_OK) {
                if (file.exists() && !file.delete()) {
                    return Boolean.FALSE;
                }

                file.getParentFile().mkdirs();

                if (!file.getParentFile().exists()) {
                    Log_OC.d(TAG, file.getParentFile().getAbsolutePath() + " does not exist");
                    return Boolean.FALSE;
                }

                if (!file.createNewFile()) {
                    Log_OC.d(TAG, file.getAbsolutePath() + " could not be created");
                    return Boolean.FALSE;
                }

                BufferedInputStream bis = new BufferedInputStream(getMethod.getResponseBodyAsStream());
                fos = new FileOutputStream(file);
                long transferred = 0;

                Header contentLength = getMethod.getResponseHeader("Content-Length");
                long totalToTransfer = contentLength != null && contentLength.getValue().length() > 0 ?
                    Long.parseLong(contentLength.getValue()) : 0;

                byte[] bytes = new byte[4096];
                int readResult;
                while ((readResult = bis.read(bytes)) != -1) {
                    fos.write(bytes, 0, readResult);
                    transferred += readResult;
                }
                // Check if the file is completed
                if (transferred != totalToTransfer) {
                    return Boolean.FALSE;
                }

                if (getMethod.getResponseBodyAsStream() != null) {
                    getMethod.getResponseBodyAsStream().close();
                }
            }
        } catch (IOException e) {
            Log_OC.e(TAG, "Error reading file", e);
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }

        return Boolean.TRUE;
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
