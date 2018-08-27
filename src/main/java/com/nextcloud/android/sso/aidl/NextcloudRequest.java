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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NextcloudRequest implements Serializable {

    private static final long serialVersionUID = 215521212534236L; //assign a long value

    public String method;
    public Map<String, List<String>> header = new HashMap<>();
    public Map<String, String> parameter = new HashMap<>();
    public String requestBody;
    public String url;
    public String token;
    public String packageName;
    public String accountName;

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

        public Builder setPackageName(String packageName) {
            ncr.packageName = packageName;
            return this;
        }

        public Builder setAccountName(String accountName) {
            ncr.accountName = accountName;
            return this;
        }
    }

    public boolean validateToken(String token) {
        // As discussed with Lukas R. at the Nextcloud Conf 2018, always compare whole strings
        // and don't exit prematurely if the string does not match anymore to prevent timing-attacks
        return isEqual(this.token.getBytes(), token.getBytes());
    }

    // Taken from http://codahale.com/a-lesson-in-timing-attacks/
    private static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
