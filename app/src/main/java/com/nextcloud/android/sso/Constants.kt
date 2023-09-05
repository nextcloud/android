/*
 * Nextcloud Android client application
 *
 * @author David Luhmer
 * @author Tobias Kaminsky
 * @author Edvard Holst
 * Copyright (C) 2019 David Luhmer
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Edvard Holst
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.android.sso;

public final class Constants {
    // Authenticator related constants
    public static final String SSO_USER_ID = "user_id";
    public static final String SSO_TOKEN = "token";
    public static final String SSO_SERVER_URL = "server_url";
    public static final String SSO_SHARED_PREFERENCE = "single-sign-on";
    public static final String NEXTCLOUD_SSO_EXCEPTION = "NextcloudSsoException";
    public static final String NEXTCLOUD_SSO = "NextcloudSSO";
    public static final String NEXTCLOUD_FILES_ACCOUNT = "NextcloudFilesAccount";
    public static final String DELIMITER = "_";

    // Custom Exceptions
    public static final String EXCEPTION_INVALID_TOKEN = "CE_1";
    public static final String EXCEPTION_ACCOUNT_NOT_FOUND = "CE_2";
    public static final String EXCEPTION_UNSUPPORTED_METHOD = "CE_3";
    public static final String EXCEPTION_INVALID_REQUEST_URL = "CE_4";
    public static final String EXCEPTION_HTTP_REQUEST_FAILED = "CE_5";
    public static final String EXCEPTION_ACCOUNT_ACCESS_DECLINED = "CE_6";

    private Constants() {
        // No instance
    }
}
