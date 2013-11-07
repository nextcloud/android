package com.owncloud.android.oc_framework.authentication;

public class AccountAuthenticatorConstants {

    public static final String KEY_AUTH_TOKEN_TYPE = "authTokenType";
    public static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    public static final String KEY_LOGIN_OPTIONS = "loginOptions";
    public static final String KEY_ACCOUNT = "account";
    
    /**
     * Value under this key should handle path to webdav php script. Will be
     * removed and usage should be replaced by combining
     * {@link com.owncloud.android.authentication.AuthenticatorActivity.KEY_OC_BASE_URL} and
     * {@link com.owncloud.android.utils.OwnCloudVersion}
     * 
     * @deprecated
     */
    public static final String KEY_OC_URL = "oc_url";
    /**
     * Version should be 3 numbers separated by dot so it can be parsed by
     * {@link com.owncloud.android.utils.OwnCloudVersion}
     */
    public static final String KEY_OC_VERSION = "oc_version";
    /**
     * Base url should point to owncloud installation without trailing / ie:
     * http://server/path or https://owncloud.server
     */
    public static final String KEY_OC_BASE_URL = "oc_base_url";
    /**
     * Flag signaling if the ownCloud server can be accessed with OAuth2 access tokens.
     */
    public static final String KEY_SUPPORTS_OAUTH2 = "oc_supports_oauth2";
    /**
     * Flag signaling if the ownCloud server can be accessed with session cookies from SAML-based web single-sign-on.
     */
    public static final String KEY_SUPPORTS_SAML_WEB_SSO = "oc_supports_saml_web_sso";

}
