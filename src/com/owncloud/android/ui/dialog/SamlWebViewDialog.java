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

package com.owncloud.android.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.authentication.SsoWebViewClient;
import com.owncloud.android.authentication.SsoWebViewClient.SsoWebViewClientListener;

import eu.alefzero.webdav.WebdavClient;

/**
 * Dialog to show the WebView for SAML Authentication
 * 
 * @author Maria Asensio
 * @author David A. Velasco
 */
public class SamlWebViewDialog extends SherlockDialogFragment {

    public final String SAML_DIALOG_TAG = "SamlWebViewDialog";
    
    private final static String TAG =  SamlWebViewDialog.class.getSimpleName();

    private static final String ARG_INITIAL_URL = "INITIAL_URL";
    private static final String ARG_TARGET_URL = "TARGET_URL";
    private static final String KEY_WEBVIEW_STATE = "WEBVIEW_STATE";
    
    private WebView mSsoWebView;
    private SsoWebViewClient mWebViewClient;
    
    private String mInitialUrl;
    private String mTargetUrl;
    
    private Handler mHandler;

    private SsoWebViewClientListener mSsoWebViewClientListener;

    //private View mSsoRootView;


    /**
     * Public factory method to get dialog instances.
     * 
     * @param handler
     * @param Url           Url to open at WebView
     * @param targetURL     mHostBaseUrl + AccountUtils.getWebdavPath(mDiscoveredVersion, mCurrentAuthTokenType)
     * @return              New dialog instance, ready to show.
     */
    public static SamlWebViewDialog newInstance(String url, String targetUrl) {
        Log_OC.d(TAG, "New instance");
        SamlWebViewDialog fragment = new SamlWebViewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_URL, url);
        args.putString(ARG_TARGET_URL, targetUrl);
        fragment.setArguments(args);
        return fragment;
    }
    
    
    public SamlWebViewDialog() {
        super();
        Log_OC.d(TAG, "constructor");
    }
    
    
    @Override
    public void onAttach(Activity activity) {
        Log_OC.d(TAG, "onAttach");
        super.onAttach(activity);
        try {
            mSsoWebViewClientListener = (SsoWebViewClientListener) activity;
            mHandler = new Handler();
            mWebViewClient = new SsoWebViewClient(mHandler, mSsoWebViewClientListener);
            
       } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + SsoWebViewClientListener.class.getSimpleName());
        }
    }

    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        CookieSyncManager.createInstance(getActivity());

        if (savedInstanceState == null) {
            mInitialUrl = getArguments().getString(ARG_INITIAL_URL);
            mTargetUrl = getArguments().getString(ARG_TARGET_URL);
        } else {
            mInitialUrl = savedInstanceState.getString(ARG_INITIAL_URL);
            mTargetUrl = savedInstanceState.getString(ARG_TARGET_URL);
        }
        
        setStyle(SherlockDialogFragment.STYLE_NO_TITLE, R.style.Theme_ownCloud_Dialog);
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateDialog");

        /*
        // build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getSherlockActivity());
        if (mSsoRootView.getParent() != null) {
            ((ViewGroup)(mSsoRootView.getParent())).removeView(mSsoRootView);
        }
        builder.setView(mSsoRootView);
        //builder.setView(mSsoWebView);
        Dialog dialog = builder.create();
        */
        
        return super.onCreateDialog(savedInstanceState);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");
        
        // Inflate layout of the dialog  
        View rootView = inflater.inflate(R.layout.sso_dialog, container, false);  // null parent view because it will go in the dialog layout
        mSsoWebView  = (WebView) rootView.findViewById(R.id.sso_webview);
            
        mWebViewClient.setTargetUrl(mTargetUrl);
        mSsoWebView.setWebViewClient(mWebViewClient);
        
        if (savedInstanceState == null) {
            Log_OC.d(TAG,  "   initWebView start");
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.removeAllCookie();
            mSsoWebView.loadUrl(mInitialUrl);
            
        } else {
            Log_OC.d(TAG, "   restoreWebView start");
            WebBackForwardList history = mSsoWebView.restoreState(savedInstanceState.getBundle(KEY_WEBVIEW_STATE));
            if (history == null) {
                Log_OC.e(TAG, "Error restoring WebView state ; back to starting URL");
                mSsoWebView.loadUrl(mInitialUrl);
            }
        }

        WebSettings webSettings = mSsoWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setSavePassword(false);
        webSettings.setUserAgentString(WebdavClient.USER_AGENT);
        webSettings.setSaveFormData(false);
        
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log_OC.d(SAML_DIALOG_TAG, "onSaveInstanceState being CALLED");
        super.onSaveInstanceState(outState);
        
        // save URLs
        outState.putString(ARG_INITIAL_URL, mInitialUrl);
        outState.putString(ARG_TARGET_URL, mTargetUrl);
        
        // Save the state of the WebView
        Bundle webviewState = new Bundle();
        mSsoWebView.saveState(webviewState);
        outState.putBundle(KEY_WEBVIEW_STATE, webviewState);
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "onDestroyView");
        
        mSsoWebView.setWebViewClient(null);
        
        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        Dialog dialog = getDialog();
        if ((dialog != null)) {
            dialog.setOnDismissListener(null);
            //dialog.dismiss();
            //dialog.setDismissMessage(null);
        }
        
        super.onDestroyView();
    }
    
    @Override
    public void onDestroy() {
        Log_OC.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log_OC.d(TAG, "onDetach");
        mSsoWebViewClientListener = null;
        mWebViewClient = null;
        super.onDetach();
    }
    
    @Override
    public void onCancel (DialogInterface dialog) {
        Log_OC.d(SAML_DIALOG_TAG, "onCancel");
        super.onCancel(dialog);
    }
    
    @Override
    public void onDismiss (DialogInterface dialog) {
        Log_OC.d(SAML_DIALOG_TAG, "onDismiss");
        super.onDismiss(dialog);
    }
    
    @Override
    public void onStart() {
        Log_OC.d(SAML_DIALOG_TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log_OC.d(SAML_DIALOG_TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onResume() {
        Log_OC.d(SAML_DIALOG_TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log_OC.d(SAML_DIALOG_TAG, "onPause");
        super.onPause();
    }
    
    @Override
    public int show (FragmentTransaction transaction, String tag) {
        Log_OC.d(SAML_DIALOG_TAG, "show (transaction)");
        return super.show(transaction, tag);
    }

    @Override
    public void show (FragmentManager manager, String tag) {
        Log_OC.d(SAML_DIALOG_TAG, "show (manager)");
        super.show(manager, tag);
    }

}