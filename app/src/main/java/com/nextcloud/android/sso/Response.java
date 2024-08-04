/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.sso;

import com.google.gson.Gson;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Response {
    private InputStream body;
    private Header[] headers;
    private HttpMethodBase method;

    public Response() {
        headers = new Header[0];
        body = new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        };
    }

    public Response(HttpMethodBase methodBase) throws IOException {
        this.method = methodBase;
        this.body = methodBase.getResponseBodyAsStream();
        this.headers = methodBase.getResponseHeaders();
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

    public HttpMethodBase getMethod() {
        return method;
    }
}
