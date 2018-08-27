package com.nextcloud.android.sso;

public class Constants {

    // Authenticator related constants
    public final static String SSO_USERNAME = "username";
    public final static String SSO_TOKEN = "token";
    public final static String SSO_SERVER_URL = "server_url";

    // Custom Exceptions
    static final String EXCEPTION_INVALID_TOKEN = "CE_1";
    static final String EXCEPTION_ACCOUNT_NOT_FOUND = "CE_2";
    static final String EXCEPTION_UNSUPPORTED_METHOD = "CE_3";
    static final String EXCEPTION_INVALID_REQUEST_URL = "CE_4";
    static final String EXCEPTION_HTTP_REQUEST_FAILED = "CE_5";
}
