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
package com.nextcloud.android.sso.aidl

import android.os.ParcelFileDescriptor
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpMethodBase
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object ParcelFileDescriptorUtil {
    @JvmStatic
    @Throws(IOException::class)
    fun pipeFrom(
        inputStream: InputStream,
        listener: IThreadListener?,
        method: HttpMethodBase?
    ): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        // start the transfer thread
        TransferThread(
            inputStream,
            ParcelFileDescriptor.AutoCloseOutputStream(writeSide),
            listener,
            method
        )
            .start()
        return readSide
    }

    class TransferThread internal constructor(
        private val inputStream: InputStream,
        private val outputStream: OutputStream,
        private val threadListener: IThreadListener?,
        private val httpMethod: HttpMethodBase?
    ) : Thread("ParcelFileDescriptor Transfer Thread") {
        init {
            isDaemon = true
        }

        override fun run() {
            val buf = ByteArray(1024)
            var len: Int
            try {
                while (inputStream.read(buf).also { len = it } > 0) {
                    outputStream.write(buf, 0, len)
                }
                outputStream.flush() // just to be safe
            } catch (e: IOException) {
                Log_OC.e(TAG, "writing failed: " + e.message)
            } finally {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    Log_OC.e(TAG, e.message)
                }
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    Log_OC.e(TAG, e.message)
                }
            }
            threadListener?.onThreadFinished(this)
            if (httpMethod != null) {
                Log_OC.i(TAG, "releaseConnection")
                httpMethod.releaseConnection()
            }
        }

        companion object {
            private val TAG = TransferThread::class.java.canonicalName
        }
    }
}