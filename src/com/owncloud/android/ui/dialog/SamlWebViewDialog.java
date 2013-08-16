package com.owncloud.android.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.owncloud.android.Log_OC;
import com.owncloud.android.authentication.SsoWebViewClient;
import com.owncloud.android.authentication.SsoWebViewClient.SsoWebViewClientListener;

import eu.alefzero.webdav.WebdavClient;

/**
 * Dialog to show the WebView for SAML Authentication
 * 
 * @author Maria Asensio
 */
public class SamlWebViewDialog extends DialogFragment
                              {

    public final String SAML_DIALOG_TAG = "SamlWebViewDialog";
    
    private final static String TAG =  SamlWebViewDialog.class.getSimpleName();
    
    private WebView mSsoWebView;
    private SsoWebViewClient mWebViewClient;
    
    private static String mUrl;
    private static String mTargetUrl;
    
    private Handler mHandler;

    private SsoWebViewClientListener mSsoWebViewClientListener;
    

    /**
     * Public factory method to get dialog instances.
     * 
     * @param handler
     * @param Url           Url to open at WebView
     * @param targetURL     mHostBaseUrl + AccountUtils.getWebdavPath(mDiscoveredVersion, mCurrentAuthTokenType)
     * @return              New dialog instance, ready to show.
     */
    public static SamlWebViewDialog newInstance(String url, String targetUrl) {
        SamlWebViewDialog fragment = new SamlWebViewDialog();
        
        mUrl = url;
        mTargetUrl = targetUrl;
        return fragment;
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        // Save the state of the WebView
        mSsoWebView.saveState(outState);
    }

    @SuppressLint("SetJavaScriptEnabled")
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log_OC.d(TAG, "On Create Dialog");

        mHandler = new Handler();
        
        mSsoWebView = new WebView(getActivity()) {
            @Override
            public boolean onCheckIsTextEditor() {
                return true; 
            }            
        };

        
        mWebViewClient = new SsoWebViewClient(mHandler, mSsoWebViewClientListener);
        mSsoWebView.setWebViewClient(mWebViewClient);
        mWebViewClient.setTargetUrl(mTargetUrl);
        
        mSsoWebView.setFocusable(true);
        mSsoWebView.setFocusableInTouchMode(true);
        mSsoWebView.setClickable(true);
        
        WebSettings webSettings = mSsoWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setSavePassword(false);
        webSettings.setUserAgentString(WebdavClient.USER_AGENT);
        
        // load the dialog
        if (savedInstanceState == null) {            
            initWebView();
        }
        else  {
            restoreWebView(savedInstanceState);
        }
        
        // build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()); 
        Dialog dialog = builder.setView(mSsoWebView).create();
        
        return dialog;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeAllCookie();

        mSsoWebView.loadUrl(mUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void restoreWebView(Bundle savedInstanceState) {
        mSsoWebView.restoreState(savedInstanceState);
        
        CookieManager cookieManager = CookieManager.getInstance();
        Log_OC.e(TAG, "Accept Cookie: " + cookieManager.acceptCookie());
    }
    
    
    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        Log_OC.d(TAG, "On Destroy");
        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        if ((dialog != null) && getRetainInstance())
            getDialog().setOnDismissListener(null);

        super.onDestroyView();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log_OC.e(TAG, "onAttach");
        try {
            mSsoWebViewClientListener = (SsoWebViewClientListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + SsoWebViewClientListener.class.getSimpleName());
        }
    }
}