package com.owncloud.android.operations;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpStatus;

import android.util.Log;

import com.owncloud.android.network.CertificateCombinedException;


public class RemoteOperationResult {
    
    public enum ResultCode {    // TODO leave alone our own errors
        OK,
        OK_SSL,
        OK_NO_SSL,
        UNHANDLED_HTTP_CODE,
        FILE_NOT_FOUND, 
        INSTANCE_NOT_CONFIGURED, 
        UNKNOWN_ERROR, 
        WRONG_CONNECTION,  
        TIMEOUT, 
        INCORRECT_ADDRESS, 
        HOST_NOT_AVAILABLE, 
        NO_NETWORK_CONNECTION, 
        SSL_ERROR,
        BAD_OC_VERSION, 
    }

    private static final String TAG = null;
    
    private boolean mSuccess = false;
    private int mHttpCode = -1;
    private Exception mException = null;
    private ResultCode mCode = ResultCode.UNKNOWN_ERROR;
    
    public RemoteOperationResult(ResultCode code) {
        mCode = code;
        mSuccess = (code == ResultCode.OK || code == ResultCode.OK_SSL || code == ResultCode.OK_NO_SSL);
    }
    
    public RemoteOperationResult(boolean success, int httpCode) {
        mSuccess = success; 
        mHttpCode = httpCode;

        if (success) {
            mCode = ResultCode.OK;
            
        } else if (httpCode > 0) {
            switch (httpCode) {
                case HttpStatus.SC_NOT_FOUND:
                    mCode = ResultCode.FILE_NOT_FOUND;
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    mCode = ResultCode.INSTANCE_NOT_CONFIGURED;
                    break;
                default:
                    mCode = ResultCode.UNHANDLED_HTTP_CODE;
            }
        }
    }
    
    public RemoteOperationResult(Exception e) {
        mException = e; 
        
        if (e instanceof SocketException) {  
            mCode = ResultCode.WRONG_CONNECTION;
            Log.e(TAG, "Socket exception", e);
        
        } else if (e instanceof SocketTimeoutException) {
            mCode = ResultCode.TIMEOUT;
            Log.e(TAG, "Socket timeout exception", e);
        
        } else if (e instanceof ConnectTimeoutException) {
            mCode = ResultCode.TIMEOUT;
            Log.e(TAG, "Connect timeout exception", e);
            
        } else if (e instanceof MalformedURLException) {
            mCode = ResultCode.INCORRECT_ADDRESS;
            Log.e(TAG, "Malformed URL exception", e);
        
        } else if (e instanceof UnknownHostException) {
            mCode = ResultCode.HOST_NOT_AVAILABLE;
            Log.e(TAG, "Unknown host exception", e);
        
        } else if (e instanceof SSLException) {
            mCode = ResultCode.SSL_ERROR;
            Log.e(TAG, "SSL exception", e);
            
        } else {
            mCode = ResultCode.UNKNOWN_ERROR;
            Log.e(TAG, "Unknown exception", e);
        }
            
        /*  }   catch (HttpException e) { // other specific exceptions from org.apache.commons.httpclient
                Log.e(TAG, "HTTP exception while trying connection", e);
            }   catch (IOException e) {   // UnkownsServiceException, and any other transport exceptions that could occur
                Log.e(TAG, "I/O exception while trying connection", e);
            }   catch (Exception e) {
                Log.e(TAG, "Unexpected exception while trying connection", e);
        */
    }
    
    
    public boolean isSuccess() {
        return mSuccess;
    }
    
    public int getHttpCode() {
        return mHttpCode;
    }
    
    public ResultCode getCode() {
        return mCode;
    }
    
    public Exception getException() {
        return mException;
    }

    public boolean isSslRecoverableException() {
        return (getSslRecoverableException() != null);
    }
    
    public CertificateCombinedException getSslRecoverableException() {
        CertificateCombinedException result = null;
        if (mCode == ResultCode.SSL_ERROR) {
            if (mException instanceof CertificateCombinedException)
                result = (CertificateCombinedException)mException;
            Throwable cause = mException.getCause();
            Throwable previousCause = null;
            while (cause != null && cause != previousCause && !(cause instanceof CertificateCombinedException)) {
                previousCause = cause;
                cause = cause.getCause();
            }
            if (cause != null && cause instanceof CertificateCombinedException)
                result = (CertificateCombinedException)cause; 
        }
        if (result != null && result.isRecoverable())
            return result;
        else
            return null;
    }

}
