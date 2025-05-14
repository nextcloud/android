/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ExternalsiteWebviewBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.NextcloudWebViewClient;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.WebViewUtil;

import java.io.InputStream;

import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This activity shows an URL as a web view
 */
public class ExternalSiteWebView extends FileActivity {
    public static final String EXTRA_TITLE = "TITLE";
    public static final String EXTRA_URL = "URL";
    public static final String EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR";
    public static final String EXTRA_SHOW_TOOLBAR = "SHOW_TOOLBAR";
    public static final String EXTRA_TEMPLATE = "TEMPLATE";

    private static final String TAG = ExternalSiteWebView.class.getSimpleName();

    protected boolean showToolbar = true;
    private ExternalsiteWebviewBinding binding;
    private boolean showSidebar;
    String url;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        bindView();
        showToolbar = showToolbarByDefault();

        Bundle extras = getIntent().getExtras();
        url = getIntent().getExtras().getString(EXTRA_URL);
        if (extras.containsKey(EXTRA_SHOW_TOOLBAR)) {
            showToolbar = extras.getBoolean(EXTRA_SHOW_TOOLBAR);
        }

        showSidebar = extras.getBoolean(EXTRA_SHOW_SIDEBAR);

        // show progress
        Window window = getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_PROGRESS);
        }

        super.onCreate(savedInstanceState);

        setContentView(getRootView());

        postOnCreate();
    }

    protected void postOnCreate() {
        final WebSettings webSettings = getWebView().getSettings();

        getWebView().setFocusable(true);
        getWebView().setFocusableInTouchMode(true);
        getWebView().setClickable(true);

        // allow debugging (when building the debug version); see details in
        // https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ||
            getResources().getBoolean(R.bool.is_beta)) {
            Log_OC.d(this, "Enable debug for webView");
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // setup toolbar
        if (showToolbar) {
            setupToolbar();
        } else {
            if (findViewById(R.id.appbar) != null) {
                findViewById(R.id.appbar).setVisibility(View.GONE);
            }
        }

        setupDrawer();

        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        String title = getIntent().getExtras().getString(EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            setupActionBar(title);
        }
        setupWebSettings(webSettings);

        final ProgressBar progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            getWebView().setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int progress) {
                    progressBar.setProgress(progress * 1000);
                }
            });
        }

        final ExternalSiteWebView self = this;
        getWebView().setWebViewClient(new NextcloudWebViewClient(getSupportFragmentManager()) {
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                InputStream resources = getResources().openRawResource(R.raw.custom_error);
                String customError = DisplayUtils.getData(resources);

                if (!customError.isEmpty()) {
                    getWebView().loadData(customError, "text/html; charset=UTF-8", null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isRedirect()) {
                    DisplayUtils.startLinkIntent(self, request.getUrl());
                    return true;
                }
                return false;
            }
        });

        new WebViewUtil(getApplicationContext()).setProxyKKPlus(getWebView());
        getWebView().loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        getWebView().destroy();
        super.onDestroy();
    }

    protected void bindView() {
        binding = ExternalsiteWebviewBinding.inflate(getLayoutInflater());
    }

    protected boolean showToolbarByDefault() {
        return true;
    }

    protected View getRootView() {
        return binding.getRoot();
    }

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebSettings(WebSettings webSettings) {
        // enable zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Non-responsive webs are zoomed out when loaded
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // user agent
        webSettings.setUserAgentString(MainApp.getUserAgent());

        // do not store private data
        webSettings.setSaveFormData(false);

        // disable local file access
        webSettings.setAllowFileAccess(false);

        // enable javascript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // caching disabled in debug mode
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }
    }

    private void setupActionBar(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            viewThemeUtils.files.themeActionBar(this, actionBar, title);

            if (showSidebar) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                setDrawerIndicatorEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (showSidebar) {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            } else {
                finish();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected WebView getWebView() {
        return binding.webView;
    }
}
