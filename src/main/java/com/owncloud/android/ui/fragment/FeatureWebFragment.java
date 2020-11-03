package com.owncloud.android.ui.fragment;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.owncloud.android.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class FeatureWebFragment extends Fragment {
    private String mWebUrl;

    static public FeatureWebFragment newInstance(String webUrl) {
        FeatureWebFragment f = new FeatureWebFragment();
        Bundle args = new Bundle();
        args.putString("url", webUrl);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebUrl = getArguments() != null ? getArguments().getString("url") : null;
    }

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.whats_new_webview_element, container, false);

        WebView webView = v.findViewById(R.id.whatsNewWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(mWebUrl);

        return v;
    }
}
