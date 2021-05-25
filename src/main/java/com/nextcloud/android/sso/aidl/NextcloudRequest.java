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

import com.nextcloud.android.sso.QueryParam;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NextcloudRequest implements Serializable {

    private static final long serialVersionUID = 215521212534240L; //assign a long value

    private String method;
    private Map<String, List<String>> header = new HashMap<>();
    private Map<String, String> parameter = new HashMap<>();
    private final Collection<QueryParam> parameterV2 = new LinkedList<>();
    private String requestBody;
    private String url;
    private String token;
    private String packageName;
    private String accountName;
    private boolean followRedirects;

    private NextcloudRequest() { }

    public static class Builder {
        private NextcloudRequest ncr;

        public Builder() {
            ncr = new NextcloudRequest();
        }

        public NextcloudRequest build() {
            return ncr;
        }

        public Builder setMethod(String method) {
            ncr.method = method;
            return this;
        }

        public Builder setHeader(Map<String, List<String>> header) {
            ncr.header = header;
            return this;
        }

        public Builder setParameter(Map<String, String> parameter) {
            ncr.parameter = parameter;
            return this;
        }

        public Builder setRequestBody(String requestBody) {
            ncr.requestBody = requestBody;
            return this;
        }

        public Builder setUrl(String url) {
            ncr.url = url;
            return this;
        }

        public Builder setToken(String token) {
            ncr.token = token;
            return this;
        }

        public Builder setAccountName(String accountName) {
            ncr.accountName = accountName;
            return this;
        }

        /**
         * Default value: true
         * @param followRedirects
         * @return
         */
        public Builder setFollowRedirects(boolean followRedirects) {
            ncr.followRedirects = followRedirects;
            return this;
        }
    }

    public String getMethod() {
        return this.method;
    }

    public Map<String, List<String>> getHeader() {
        return this.header;
    }

    public Map<String, String> getParameter() {
        return this.parameter;
    }

    public String getRequestBody() {
        return this.requestBody;
    }

    public String getUrl() {
        return this.url;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAccountName() {
        return this.accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public boolean isFollowRedirects() {
        return this.followRedirects;
    }

    public Collection<QueryParam> getParameterV2() {
        return parameterV2;
    }
}
