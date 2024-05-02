/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 David Luhmer <david-dev@live.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.sso.aidl;

import android.os.ParcelFileDescriptor;

import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpMethodBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ParcelFileDescriptorUtil {

    private ParcelFileDescriptorUtil() { }

    public static ParcelFileDescriptor pipeFrom(InputStream inputStream,
                                                IThreadListener listener,
                                                HttpMethodBase method)
            throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        // start the transfer thread
        new TransferThread(inputStream,
                           new ParcelFileDescriptor.AutoCloseOutputStream(writeSide),
                           listener,
                           method)
                .start();

        return readSide;
    }

    public static class TransferThread extends Thread {
        private static final String TAG = TransferThread.class.getCanonicalName();
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final IThreadListener threadListener;
        private final HttpMethodBase httpMethod;

        TransferThread(InputStream in, OutputStream out, IThreadListener listener, HttpMethodBase method) {
            super("ParcelFileDescriptor Transfer Thread");
            inputStream = in;
            outputStream = out;
            threadListener = listener;
            httpMethod = method;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int len;

            try {
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.flush(); // just to be safe
            } catch (IOException e) {
                Log_OC.e(TAG, "writing failed: " + e.getMessage());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, e.getMessage());
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, e.getMessage());
                }
            }
            if (threadListener != null) {
                threadListener.onThreadFinished(this);
            }

            if (httpMethod != null) {
                Log_OC.i(TAG, "releaseConnection");
                httpMethod.releaseConnection();
            }
        }
    }
}
