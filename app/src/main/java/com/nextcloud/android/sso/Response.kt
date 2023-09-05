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
package com.nextcloud.android.sso

import com.google.gson.Gson
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpMethodBase
import java.io.InputStream

class Response {
    var body: InputStream
        private set
    private var headers: Array<Header?>
    var method: HttpMethodBase? = null
        private set

    constructor() {
        headers = arrayOfNulls(0)
        body = object : InputStream() {
            override fun read(): Int {
                return 0
            }
        }
    }

    constructor(methodBase: HttpMethodBase) {
        method = methodBase
        body = methodBase.responseBodyAsStream
        headers = methodBase.responseHeaders
    }

    val plainHeadersString: String
        get() {
            val arrayList: MutableList<PlainHeader> = ArrayList(headers.size)
            for (header in headers) {
                arrayList.add(PlainHeader(header!!.name, header.value))
            }
            val gson = Gson()
            return gson.toJson(arrayList)
        }
}