/*
 *
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

package com.nextcloud.android.sso;

import com.google.gson.Gson;

import org.apache.commons.httpclient.Header;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Response {
    private InputStream body;
    private Header[] headers;

    public Response() {
        headers = new Header[0];
        body = new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        };
    }

    public Response(InputStream inputStream, Header[] headers) {
        this.body = inputStream;
        this.headers = headers;
    }

    public String getPlainHeadersString() {
        List<PlainHeader> arrayList = new ArrayList<>(headers.length);

        for (Header header : headers) {
            arrayList.add(new PlainHeader(header.getName(), header.getValue()));
        }

        Gson gson = new Gson();
        return gson.toJson(arrayList);
    }

    public InputStream getBody() {
        return this.body;
    }
}
