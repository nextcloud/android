package de.luhmer.owncloud.accountimporter.helper;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ParcelFileDescriptorUtil {

    public static ParcelFileDescriptor pipeFrom(InputStream inputStream, IThreadListener listener)
            throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        // start the transfer thread
        new TransferThread(inputStream, new ParcelFileDescriptor.AutoCloseOutputStream(writeSide),
                listener)
                .start();

        return readSide;
    }

    public static ParcelFileDescriptor pipeTo(OutputStream outputStream, IThreadListener listener)
            throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        // start the transfer thread
        new TransferThread(new ParcelFileDescriptor.AutoCloseInputStream(readSide), outputStream,
                listener)
                .start();

        return writeSide;
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;
        final IThreadListener mListener;

        TransferThread(InputStream in, OutputStream out, IThreadListener listener) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            mListener = listener;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int len;

            try {
                while ((len = mIn.read(buf)) > 0) {
                    mOut.write(buf, 0, len);
                }
                mOut.flush(); // just to be safe
            } catch (IOException e) {
                Log.e("TransferThread", "writing failed");
                e.printStackTrace();
            } finally {
                try {
                    mIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    mOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mListener != null)
                mListener.onThreadFinished(this);
        }
    }
}
