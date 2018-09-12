package com.nextcloud.android.sso;

public class Constants {

    // Authenticator related constants
    public static final String SSO_USERNAME = "username";
    public static final String SSO_TOKEN = "token";
    public static final String SSO_SERVER_URL = "server_url";
    public static final String SSO_SHARED_PREFERENCE = "single-sign-on";
    public static final String NEXTCLOUD_SSO_EXCEPTION = "NextcloudSsoException";
    public static final String NEXTCLOUD_SSO = "NextcloudSSO";
    public static final String NEXTCLOUD_FILES_ACCOUNT = "NextcloudFilesAccount";


    // Custom Exceptions
    public static final String EXCEPTION_INVALID_TOKEN = "CE_1";
    public static final String EXCEPTION_ACCOUNT_NOT_FOUND = "CE_2";
    public static final String EXCEPTION_UNSUPPORTED_METHOD = "CE_3";
    public static final String EXCEPTION_INVALID_REQUEST_URL = "CE_4";
    public static final String EXCEPTION_HTTP_REQUEST_FAILED = "CE_5";
    public static final String EXCEPTION_ACCOUNT_ACCESS_DECLINED = "CE_6";
}
