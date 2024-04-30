/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2017 David Luhmer <david-dev@live.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
