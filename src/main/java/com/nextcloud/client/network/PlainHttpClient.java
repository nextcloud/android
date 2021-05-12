/*
 *
 *  * Nextcloud Android client application
 *  *
 *  * @author Tobias Kaminsky
 *  * Copyright (C) 2019 Tobias Kaminsky
 *  * Copyright (C) 2019 Nextcloud GmbH
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.network;

import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;

public class PlainHttpClient extends HttpClient {

    @Override
    public int executeMethod(HttpMethod method) throws IOException {
        method.getParams().setParameter(HttpMethodParams.USER_AGENT, OwnCloudClientManagerFactory.getUserAgent());

        return super.executeMethod(method);
    }
}
