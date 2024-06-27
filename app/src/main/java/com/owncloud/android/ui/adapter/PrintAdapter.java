/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PrintAdapter extends PrintDocumentAdapter {
    private static final String TAG = PrintAdapter.class.getSimpleName();
    private static final String PDF_NAME = "finalPrint.pdf";

    private final String filePath;

    public PrintAdapter(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle extras) {
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
        } else {
            PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(PDF_NAME);
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build();
            callback.onLayoutFinished(builder.build(), !newAttributes.equals(oldAttributes));
        }
    }


    @Override
    public void onWrite(PageRange[] pages,
                        ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {

        try (InputStream in = new FileInputStream(filePath);
             OutputStream out = new FileOutputStream(destination.getFileDescriptor())) {

            byte[] buf = new byte[16384];
            int size;

            while ((size = in.read(buf)) >= 0 && !cancellationSignal.isCanceled()) {
                out.write(buf, 0, size);
            }

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
            } else {
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }

        } catch (IOException e) {
            Log_OC.e(TAG, "Error using temp file", e);
        }
    }
}
