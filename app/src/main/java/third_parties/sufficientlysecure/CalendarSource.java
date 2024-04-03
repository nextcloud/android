/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package third_parties.sufficientlysecure;

import android.content.Context;
import android.net.Uri;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class CalendarSource {
    private static final String HTTP_SEP = "://";

    private URL mUrl = null;
    private Uri mUri = null;
    private final String mString;
    private final String mUsername;
    private final String mPassword;
    private final Context context;

    public CalendarSource(String url,
                          Uri uri,
                          String username,
                          String password,
                          Context context) throws MalformedURLException {
        if (url != null) {
            mUrl = new URL(url);
            mString = mUrl.toString();
        } else {
            mUri = uri;
            mString = uri.toString();
        }
        mUsername = username;
        mPassword = password;
        this.context = context;
    }

    public URLConnection getConnection() throws IOException {
        if (mUsername != null) {
            String protocol = mUrl.getProtocol();
            String userPass = mUsername + ":" + mPassword;

            if (protocol.equalsIgnoreCase("ftp") || protocol.equalsIgnoreCase("ftps")) {
                String external = mUrl.toExternalForm();
                String end = external.substring(protocol.length() + HTTP_SEP.length());
                return new URL(protocol + HTTP_SEP + userPass + "@" + end).openConnection();
            }

            if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
                String encoded = new String(new Base64().encode(userPass.getBytes("UTF-8")));
                URLConnection connection = mUrl.openConnection();
                connection.setRequestProperty("Authorization", "Basic " + encoded);
                return connection;
            }
        }
        return mUrl.openConnection();
    }

    public InputStream getStream() throws IOException {
        if (mUri != null) {
            return context.getContentResolver().openInputStream(mUri);
        }
        URLConnection c = this.getConnection();
        return c == null ? null : c.getInputStream();
    }

    @Override
    public String toString() {
        return mString;
    }
}
