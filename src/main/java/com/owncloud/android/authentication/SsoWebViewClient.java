/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.authentication;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


/**
 * Custom {@link WebViewClient} client aimed to catch the end of a single-sign-on process 
 * running in the {@link WebView} that is attached to.
 * 
 * Assumes that the single-sign-on is kept thanks to a cookie set at the end of the
 * authentication process.
 */
public class SsoWebViewClient extends WebViewClient {
        
    private static final String TAG = SsoWebViewClient.class.getSimpleName();

    private Context mContext;
    private Handler mListenerHandler;
    private WeakReference<SsoWebViewClientListener> mListenerRef;
    private String mTargetUrl;
    private String mLastReloadedUrlAtError;
    
    public interface SsoWebViewClientListener {
        void onSsoFinished(String sessionCookie);
    }

    public SsoWebViewClient (Context context, Handler listenerHandler, SsoWebViewClientListener listener) {
        mContext = context;
        mListenerHandler = listenerHandler;
        mListenerRef = new WeakReference<SsoWebViewClient.SsoWebViewClientListener>(listener);
        mTargetUrl = "fake://url.to.be.set";
        mLastReloadedUrlAtError = null;
    }
    
    public String getTargetUrl() {
        return mTargetUrl;
    }
    
    public void setTargetUrl(String targetUrl) {
        mTargetUrl = targetUrl;
    }

    @Override
    public void onPageStarted (WebView view, String url, Bitmap favicon) {
        Log_OC.d(TAG, "onPageStarted : " + url);
        view.clearCache(true);
        super.onPageStarted(view, url, favicon);
    }
    
    @Override
    public void onFormResubmission (WebView view, Message dontResend, Message resend) {
        Log_OC.d(TAG, "onFormResubMission ");

        // necessary to grant reload of last page when device orientation is changed after sending a form
        resend.sendToTarget();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }
    
    @Override
    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
        Log_OC.e(TAG, "onReceivedError : " + failingUrl + ", code " + errorCode + ", description: " + description);
        if (!failingUrl.equals(mLastReloadedUrlAtError)) {
            view.reload();
            mLastReloadedUrlAtError = failingUrl;
        } else {
            mLastReloadedUrlAtError = null;
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }
    
    @Override
    public void onPageFinished (WebView view, String url) {
        Log_OC.d(TAG, "onPageFinished : " + url);
        mLastReloadedUrlAtError = null;
        if (url.startsWith(mTargetUrl)) {
            view.setVisibility(View.GONE);
            CookieManager cookieManager = CookieManager.getInstance();
            final String cookies = cookieManager.getCookie(url);
            //Log_OC.d(TAG, "Cookies: " + cookies);
            if (mListenerHandler != null && mListenerRef != null) {
                // this is good idea because onPageFinished is not running in the UI thread
                mListenerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SsoWebViewClientListener listener = mListenerRef.get();
                        if (listener != null) {
                        	// Send Cookies to the listener
                            listener.onSsoFinished(cookies);
                        }
                    }
                });
            }
        } 
    }
    
    @Override
    public void onReceivedSslError (final WebView view, final SslErrorHandler handler, SslError error) {
        Log_OC.e(TAG, "onReceivedSslError : " + error);
        // Test 1
        X509Certificate x509Certificate = getX509CertificateFromError(error);
        boolean isKnownServer = false;
        
        if (x509Certificate != null) {
            try {
                isKnownServer = NetworkUtils.isCertInKnownServersStore(x509Certificate, mContext);
            } catch (Exception e) {
                Log_OC.e(TAG, "Exception: " + e.getMessage());
            }
        }
        
         if (isKnownServer) {
             handler.proceed();
         } else {
             ((AuthenticatorActivity)mContext).showUntrustedCertDialog(x509Certificate, error, handler);
         }
    }
    
    /**
     * Obtain the X509Certificate from SslError
     * @param   error     SslError
     * @return  X509Certificate from error
     */
    public static X509Certificate getX509CertificateFromError(SslError error) {
        Bundle bundle = SslCertificate.saveState(error.getCertificate());
        X509Certificate x509Certificate;
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            x509Certificate = null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) cert;
            } catch (CertificateException e) {
                x509Certificate = null;
            }
        }        
        return x509Certificate;
    }
    
    @Override
    public void onReceivedHttpAuthRequest (WebView view, HttpAuthHandler handler, String host, String realm) {
        Log_OC.d(TAG, "onReceivedHttpAuthRequest : " + host);

        ((AuthenticatorActivity)mContext).createAuthenticationDialog(view, handler);
    }
}
