package com.owncloud.android.authenticator;

public interface OnConnectCheckListener {

    enum ResultType {
        OK_SSL, OK_NO_SSL, SSL_INIT_ERROR, HOST_NOT_AVAILABLE, TIMEOUT, NO_NETWORK_CONNECTION, INCORRECT_ADDRESS, INSTANCE_NOT_CONFIGURED, FILE_NOT_FOUND, UNKNOWN_ERROR, WRONG_CONNECTION,  SSL_UNVERIFIED_SERVER, BAD_OC_VERSION
    }

    public void onConnectionCheckResult(ResultType type);

}
