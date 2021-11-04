/*
 *  Nextcloud SingleSignOn
 *
 *  @author David Luhmer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
