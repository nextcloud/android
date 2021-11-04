/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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
