package com.owncloud.android.ui.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.owncloud.android.Log_OC;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.authentication.SsoWebViewClient;
import com.owncloud.android.authentication.SsoWebViewClient.SsoWebViewClientListener;

import eu.alefzero.webdav.WebdavClient;

/**
 * Dialog to show the WebView for SAML Authentication
 * 
 * @author Maria Asensio
 */
public class SamlWebViewDialog extends DialogFragment
                               implements SsoWebViewClientListener{

    public final String SAML_DIALOG_TAG = "SamlWebViewDialog";
    
    private final static String TAG =  SamlWebViewDialog.class.getSimpleName();
    
    private WebView mSsoWebView;
    private SsoWebViewClient mWebViewClient;
    
    private static String mUrl;
    private static String mTargetUrl;
    
    private static Handler mHandler;
    

    /**
     * Public factory method to get dialog instances.
     * 
     * @param handler
     * @param Url           Url to open at WebView
     * @param targetURL     mHostBaseUrl + AccountUtils.getWebdavPath(mDiscoveredVersion, mCurrentAuthTokenType)
     * @return              New dialog instance, ready to show.
     */
    public static SamlWebViewDialog newInstance(Handler handler,String url, String targetUrl) {
        SamlWebViewDialog fragment = new SamlWebViewDialog();
        mHandler = handler;
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


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log_OC.d(TAG, "On Create Dialog");

        /// load the dialog
        initWebView(savedInstanceState);
        setRetainInstance(true);
        /// build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        Dialog dialog = builder.setView(mSsoWebView).create();
        
        return dialog;
    }

    
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(Bundle savedInstanceState) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        //cookieManager.removeSessionCookie();        

        mWebViewClient = new SsoWebViewClient(mHandler, this);
        mWebViewClient.setTargetUrl(mTargetUrl);
        if (savedInstanceState == null) {
            
            Log_OC.d(TAG, "Saved Instance State NULL");
            mSsoWebView = new WebView(getActivity()) {
                @Override
                public boolean onCheckIsTextEditor() {
                    return true; 
                }            
            };
            
            mSsoWebView.setWebViewClient(mWebViewClient);
            mSsoWebView.setFocusable(true);
            mSsoWebView.setFocusableInTouchMode(true);
            mSsoWebView.setClickable(true);
            
            WebSettings webSettings = mSsoWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setLoadWithOverviewMode(false);
            webSettings.setSavePassword(false);
            webSettings.setUserAgentString(WebdavClient.USER_AGENT);
            
            mSsoWebView.loadUrl(mUrl);
        }
        else {
            Log_OC.d(TAG, "Saved Instance State NOT NULL");
            
            mSsoWebView.restoreState(savedInstanceState);
        }
        
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
    public void onSsoFinished(String sessionCookie) {
        //Toast.makeText(this, "got cookies: " + sessionCookie, Toast.LENGTH_LONG).show();

        if (sessionCookie != null && sessionCookie.length() > 0) {
            Log_OC.d(TAG, "Successful SSO - time to save the account");
            ((AuthenticatorActivity) getActivity()).onSamlDialogSuccess(sessionCookie);
            dismiss();

        } else { 
            // TODO - show fail
            Log_OC.d(TAG, "SSO failed");
        }
    }
    
}