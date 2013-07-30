/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.owncloud.android.Log_OC;

/**
 * Custom {@link WebViewClient} client aimed to catch the end of a single-sign-on process 
 * running in the {@link WebView} that is attached to.
 * 
 * Assumes that the single-sign-on is kept thanks to a cookie set at the end of the
 * authentication process.
 *   
 * @author David A. Velasco
 */
public class SsoWebViewClient extends WebViewClient {
        
    private static final String TAG = SsoWebViewClient.class.getSimpleName();
    
    private Context mContext;
    private String mTargetUrl;
    
    public SsoWebViewClient (Context context) {
        mContext = context;
        mTargetUrl = "fake://url.to.be.set";
    }
    
    public String getTargetUrl() {
        return mTargetUrl;
    }
    
    public void setTargetUrl(String targetUrl) {
        mTargetUrl = targetUrl;
    }

    @Override
    public void onPageStarted (WebView view, String url, Bitmap favicon) {
        //Log_OC.e(TAG, "onPageStarted : " + url);
        if (url.startsWith(mTargetUrl)) {
            view.setVisibility(View.GONE);
            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(url);
            Toast.makeText(mContext, "got cookies: " + cookies, Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        //view.loadUrl(url);
        return false;
    }
    
    @Override
    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
        Log_OC.e(TAG, "onReceivedError : " + failingUrl);
    }
    
    /*

    @Override
    public void doUpdateVisitedHistory (WebView view, String url, boolean isReload) {
        Log_OC.e(TAG, "doUpdateVisitedHistory : " + url);
    }
    
    @Override
    public void onPageFinished (WebView view, String url) {
        Log_OC.e(TAG, "onPageFinished : " + url);
    }
    
    @Override
    public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
        Log_OC.e(TAG, "onReceivedSslError : " + error);
    }
    
    @Override
    public void onReceivedHttpAuthRequest (WebView view, HttpAuthHandler handler, String host, String realm) {
        Log_OC.e(TAG, "onReceivedHttpAuthRequest : " + host);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest (WebView view, String url) {
        Log_OC.e(TAG, "shouldInterceptRequest : " + url);
        return null;
    }
    
    @Override
    public void onLoadResource (WebView view, String url) {
        Log_OC.e(TAG, "onLoadResource : " + url);            
    }
    
    @Override
    public void onFormResubmission (WebView view, Message dontResend, Message resend) {
        Log_OC.e(TAG, "onFormResubMission ");
        super.onFormResubmission(view, dontResend, resend);
    }
    
    @Override
    public void onReceivedLoginRequest (WebView view, String realm, String account, String args) {
        Log_OC.e(TAG, "onReceivedLoginRequest : " + realm + ", " + account + ", " + args);
    }
    
    @Override
    public void onScaleChanged (WebView view, float oldScale, float newScale) {
        Log_OC.e(TAG, "onScaleChanged : " + oldScale + " -> " + newScale);
    }
    
    @Override
    public void onUnhandledKeyEvent (WebView view, KeyEvent event) {
        Log_OC.e(TAG, "onUnhandledKeyEvent : " + event);
    }
    
    @Override
    public boolean shouldOverrideKeyEvent (WebView view, KeyEvent event) {
        Log_OC.e(TAG, "shouldOverrideKeyEvent : " + event);
        return false;
    }
    
    */
}
