/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author Mario Danic
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * All changes by Mario Danic are distributed under the following terms:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.authentication;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AndroidRuntimeException;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.onboarding.OnboardingService;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.DetectAuthenticationMethodOperation.AuthenticationMethod;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.components.CustomEditText;
import com.owncloud.android.ui.dialog.CredentialsDialogFragment;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.PermissionUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This Activity is used to add an ownCloud account to the App
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
    implements OnRemoteOperationListener, OnFocusChangeListener, OnEditorActionListener, OnSslUntrustedCertListener,
    AuthenticatorAsyncTask.OnAuthenticatorTaskListener, Injectable {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_USE_PROVIDER_AS_WEBLOGIN = "USE_PROVIDER_AS_WEBLOGIN";

    private static final String KEY_HOST_URL_TEXT = "HOST_URL_TEXT";
    private static final String KEY_OC_VERSION = "OC_VERSION";
    private static final String KEY_SERVER_VALID = "SERVER_VALID";
    private static final String KEY_SERVER_CHECKED = "SERVER_CHECKED";
    private static final String KEY_SERVER_STATUS_TEXT = "SERVER_STATUS_TEXT";
    private static final String KEY_SERVER_STATUS_ICON = "SERVER_STATUS_ICON";
    private static final String KEY_IS_SSL_CONN = "IS_SSL_CONN";
    private static final String KEY_PASSWORD_EXPOSED = "PASSWORD_VISIBLE";
    private static final String KEY_AUTH_STATUS_TEXT = "AUTH_STATUS_TEXT";
    private static final String KEY_AUTH_STATUS_ICON = "AUTH_STATUS_ICON";
    private static final String KEY_SERVER_AUTH_METHOD = "SERVER_AUTH_METHOD";
    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";

    public static final byte ACTION_CREATE = 0;
    public static final byte ACTION_UPDATE_EXPIRED_TOKEN = 2;       // detected by the app

    private static final String UNTRUSTED_CERT_DIALOG_TAG = "UNTRUSTED_CERT_DIALOG";
    private static final String WAIT_DIALOG_TAG = "WAIT_DIALOG";
    private static final String CREDENTIALS_DIALOG_TAG = "CREDENTIALS_DIALOG";
    private static final String KEY_AUTH_IS_FIRST_ATTEMPT_TAG = "KEY_AUTH_IS_FIRST_ATTEMPT";

    private static final String KEY_USERNAME = "USERNAME";
    private static final String KEY_PASSWORD = "PASSWORD";
    private static final String KEY_ASYNC_TASK_IN_PROGRESS = "AUTH_IN_PROGRESS";
    private static final String WEB_LOGIN = "/index.php/login/flow";
    public static final String PROTOCOL_SUFFIX = "://";
    public static final String LOGIN_URL_DATA_KEY_VALUE_SEPARATOR = ":";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HTTP_PROTOCOL = "http://";

    public static final String REGULAR_SERVER_INPUT_TYPE = "regular";
    public static final String SUBDOMAIN_SERVER_INPUT_TYPE = "prefix";
    public static final String DIRECTORY_SERVER_INPUT_TYPE = "suffix";
    public static final int NO_ICON = 0;
    public static final String EMPTY_STRING = "";

    private static final int REQUEST_CODE_QR_SCAN = 101;
    public static final int REQUEST_CODE_FIRST_RUN = 102;


    /// parameters from EXTRAs in starter Intent
    private byte mAction;
    private Account mAccount;

    /// activity-level references / state
    private final Handler mHandler = new Handler();
    private ServiceConnection mOperationsServiceConnection;
    private OperationsServiceBinder mOperationsServiceBinder;
    private AccountManager mAccountMgr;

    /// Server PRE-Fragment elements
    private CustomEditText mHostUrlInput;
    private View mRefreshButton;
    private TextView mServerStatusView;

    private TextWatcher mHostUrlInputWatcher;
    private String mServerStatusText = EMPTY_STRING;
    private int mServerStatusIcon;

    private boolean mServerIsChecked;
    private boolean mServerIsValid;

    private GetServerInfoOperation.ServerInfo mServerInfo = new GetServerInfoOperation.ServerInfo();

    /// Authentication PRE-Fragment elements
    private EditText mUsernameInput;
    private EditText mPasswordInput;
    private View mOkButton;
    private TextView mAuthStatusView;
    private ImageButton mTestServerButton;

    private WebView mLoginWebView;

    private String mAuthStatusText = EMPTY_STRING;
    private int mAuthStatusIcon;

    private String mAuthToken = EMPTY_STRING;
    private AuthenticatorAsyncTask mAsyncTask;

    private boolean mIsFirstAuthAttempt;

    /// Identifier of operation in progress which result shouldn't be lost
    private long mWaitingForOpId = Long.MAX_VALUE;

    private boolean webViewLoginMethod;
    private String webViewUser;
    private String webViewPassword;
    private TextInputLayout mUsernameInputLayout;
    private TextInputLayout mPasswordInputLayout;
    private boolean forceOldLoginMethod;

    @Inject UserAccountManager accountManager;
    @Inject AppPreferences preferences;
    @Inject OnboardingService onboarding;
    @Inject DeviceInfo deviceInfo;

    /**
     * {@inheritDoc}
     *
     * IMPORTANT ENTRY POINT 1: activity is shown to the user
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log_OC.e(TAG,  "onCreate init");
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        boolean directLogin = data != null && data.toString().startsWith(getString(R.string.login_data_own_scheme));
        if (savedInstanceState == null && !directLogin) {
            onboarding.launchFirstRunIfNeeded(this);
        }

        // delete cookies for webView
        deleteCookies();

        // Workaround, for fixing a problem with Android Library Support v7 19
        //getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mIsFirstAuthAttempt = true;

        /// init activity state
        mAccountMgr = AccountManager.get(this);

        /// get input values
        mAction = getIntent().getByteExtra(EXTRA_ACTION, ACTION_CREATE);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            mAccount = extras.getParcelable(EXTRA_ACCOUNT);
        }

        if (savedInstanceState != null) {
            mWaitingForOpId = savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID);
            mIsFirstAuthAttempt = savedInstanceState.getBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG);
        }

        String webloginUrl = null;
        boolean showLegacyLogin;
        if (getIntent().getBooleanExtra(EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)) {
            webViewLoginMethod = true;
            webloginUrl = getString(R.string.provider_registration_server);
            showLegacyLogin = false;
        } else {
            webViewLoginMethod = !TextUtils.isEmpty(getResources().getString(R.string.webview_login_url));
            showLegacyLogin = true;
        }

        if (webViewLoginMethod) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        /// load user interface
        if (!webViewLoginMethod) {
            setContentView(R.layout.account_setup);

            /// initialize general UI elements
            initOverallUi();

            findViewById(R.id.centeredRefreshButton).setOnClickListener(v -> checkOcServer());

            findViewById(R.id.embeddedRefreshButton).setOnClickListener(v -> checkOcServer());

            /// initialize block to be moved to single Fragment to check server and get info about it

            /// initialize block to be moved to single Fragment to retrieve and validate credentials
            initAuthorizationPreFragment(savedInstanceState);

        } else {
            setContentView(R.layout.account_setup_webview);
            mLoginWebView = findViewById(R.id.login_webview);
            initWebViewLogin(webloginUrl, showLegacyLogin, false);
        }

        initServerPreFragment(savedInstanceState);
    }

    private void deleteCookies() {
        try {
            CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies(null);
            } else {
                cookieManager.removeAllCookie();
            }
        } catch (AndroidRuntimeException e) {
            Log_OC.e(TAG, e.getMessage());
        }
    }

    private static String getWebLoginUserAgent() {
        return Build.MANUFACTURER.substring(0, 1).toUpperCase(Locale.getDefault()) +
                Build.MANUFACTURER.substring(1).toLowerCase(Locale.getDefault()) + " " + Build.MODEL;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewLogin(String baseURL, boolean showLegacyLogin, boolean useGenericUserAgent) {
        mLoginWebView.setVisibility(View.GONE);

        final ProgressBar progressBar = findViewById(R.id.login_webview_progress_bar);

        mLoginWebView.getSettings().setAllowFileAccess(false);
        mLoginWebView.getSettings().setJavaScriptEnabled(true);
        mLoginWebView.getSettings().setDomStorageEnabled(true);

        if (useGenericUserAgent) {
            mLoginWebView.getSettings().setUserAgentString(MainApp.getUserAgent());
        } else {
            mLoginWebView.getSettings().setUserAgentString(getWebLoginUserAgent());
        }
        mLoginWebView.getSettings().setSaveFormData(false);
        mLoginWebView.getSettings().setSavePassword(false);

        Map<String, String> headers = new HashMap<>();
        headers.put(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

        String url;
        if (baseURL != null && !baseURL.isEmpty()) {
            url = baseURL;
        } else {
            url = getResources().getString(R.string.webview_login_url);
        }

        mLoginWebView.loadUrl(url, headers);

        setClient(progressBar);

        // show snackbar after 60s to switch back to old login method
        if (showLegacyLogin) {
            final String finalBaseURL = baseURL;
            new Handler().postDelayed(() -> DisplayUtils.createSnackbar(mLoginWebView,
                                                                        R.string.fallback_weblogin_text,
                                                                        Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(getResources().getColor(R.color.white))
                .setAction(R.string.fallback_weblogin_back, v -> {
                    mLoginWebView.setVisibility(View.INVISIBLE);
                    webViewLoginMethod = false;

                    setContentView(R.layout.account_setup);

                    // initialize general UI elements
                    initOverallUi();

                    mPasswordInputLayout.setVisibility(View.VISIBLE);
                    mUsernameInputLayout.setVisibility(View.VISIBLE);
                    mUsernameInput.requestFocus();
                    mAuthStatusView.setVisibility(View.INVISIBLE);
                    mServerStatusView.setVisibility(View.INVISIBLE);
                    mTestServerButton.setVisibility(View.INVISIBLE);
                    forceOldLoginMethod = true;
                    mOkButton.setVisibility(View.VISIBLE);

                    initServerPreFragment(null);

                    if (finalBaseURL != null) {
                        mHostUrlInput.setText(finalBaseURL.replace(WEB_LOGIN, ""));
                    } else {
                        mHostUrlInput.setText(finalBaseURL);
                    }

                    checkOcServer();
                }).show(), 60 * 1000);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mLoginWebView != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mLoginWebView.canGoBack()) {
                        mLoginWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    private void setClient(ProgressBar progressBar) {
        mLoginWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/")) {
                    parseAndLoginFromWebView(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                progressBar.setVisibility(View.GONE);
                mLoginWebView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                X509Certificate cert = getX509CertificateFromError(error);

                try {
                    if (cert != null && NetworkUtils.isCertInKnownServersStore(cert, getApplicationContext())) {
                        handler.proceed();
                    } else {
                        showUntrustedCertDialog(cert, error, handler);
                    }
                } catch (Exception e) {
                    Log_OC.e(TAG, "Cert could not be verified");
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                mLoginWebView.setVisibility(View.VISIBLE);

                InputStream resources = getResources().openRawResource(R.raw.custom_error);
                String customError = DisplayUtils.getData(resources);

                if (!customError.isEmpty()) {
                    mLoginWebView.loadData(customError, "text/html; charset=UTF-8", null);
                }
            }
        });
    }

    private void parseAndLoginFromWebView(String dataString) {
        String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
        LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, dataString);

        if (loginUrlInfo != null) {
            try {
                mServerInfo.mBaseUrl = AuthenticatorUrlUtils.normalizeUrlSuffix(loginUrlInfo.serverAddress);
                webViewUser = loginUrlInfo.username;
                webViewPassword = loginUrlInfo.password;
            } catch (Exception e) {
                mServerStatusIcon = R.drawable.ic_alert;
                mServerStatusText = "QR Code could not be read!";
                showServerStatus();
            }
            checkOcServer();
        }
    }

    private void populateLoginFields(String dataString) throws IllegalArgumentException {
        // check if it is cloud://login/
        if (dataString.startsWith(getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/")) {
            String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
            LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, dataString);

            if (loginUrlInfo != null) {
                mHostUrlInput.setText(loginUrlInfo.serverAddress);
                mUsernameInput.setText(loginUrlInfo.username);
                mPasswordInput.setText(loginUrlInfo.password);

                if (loginUrlInfo.serverAddress != null && !mServerIsChecked) {
                    onUrlInputFocusLost();
                }
            }
        }
    }

    /**
     * parses a URI string and returns a login data object with the information from the URI string.
     *
     * @param prefix     URI beginning, e.g. cloud://login/
     * @param dataString the complete URI
     * @return login data
     * @throws IllegalArgumentException when
     */
    public static LoginUrlInfo parseLoginDataUrl(String prefix, String dataString) throws IllegalArgumentException {
        if (dataString.length() < prefix.length()) {
            throw new IllegalArgumentException("Invalid login URL detected");
        }

        // format is basically xxx://login/server:xxx&user:xxx&password while all variables are optional
        String data = dataString.substring(prefix.length());

        // parse data
        String[] values = data.split("&");

        if (values.length < 1 || values.length > 3) {
            // error illegal number of URL elements detected
            throw new IllegalArgumentException("Illegal number of login URL elements detected: " + values.length);
        }

        LoginUrlInfo loginUrlInfo = new LoginUrlInfo();

        for (String value : values) {
            if (value.startsWith("user" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR)) {
                loginUrlInfo.username = URLDecoder.decode(
                        value.substring(("user" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR).length()));
            } else if (value.startsWith("password" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR)) {
                loginUrlInfo.password = URLDecoder.decode(
                        value.substring(("password" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR).length()));
            } else if (value.startsWith("server" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR)) {
                loginUrlInfo.serverAddress = URLDecoder.decode(
                        value.substring(("server" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR).length()));
            }
        }

        return loginUrlInfo;
    }

    /**
     * Configures elements in the user interface under direct control of the Activity.
     */
    private void initOverallUi() {
        mHostUrlInput = findViewById(R.id.hostUrlInput);
        mUsernameInputLayout = findViewById(R.id.input_layout_account_username);
        mPasswordInputLayout = findViewById(R.id.input_layout_account_password);
        mPasswordInput = findViewById(R.id.account_password);
        mUsernameInput = findViewById(R.id.account_username);
        mAuthStatusView = findViewById(R.id.auth_status_text);
        mServerStatusView = findViewById(R.id.server_status_text);
        mTestServerButton = findViewById(R.id.testServerButton);

        mOkButton = findViewById(R.id.buttonOK);
        mOkButton.setOnClickListener(v -> onOkClick());

        ImageButton scanQR = findViewById(R.id.scanQR);
        if (deviceInfo.hasCamera(this)) {
            scanQR.setOnClickListener(v -> onScan());
        } else {
            scanQR.setVisibility(View.GONE);
        }

        setupInstructionMessage();

        mTestServerButton.setVisibility(mAction == ACTION_CREATE ? View.VISIBLE : View.GONE);
    }


    private void setupInstructionMessage() {
        TextView instructionsView = findViewById(R.id.instructions_message);

        if (mAction == ACTION_UPDATE_EXPIRED_TOKEN) {
            instructionsView.setVisibility(View.VISIBLE);

            String instructionsMessageText = getString(R.string.auth_expired_basic_auth_toast);
            instructionsView.setText(instructionsMessageText);
        } else {
            instructionsView.setVisibility(View.GONE);
        }
    }

    public void onTestServerConnectionClick(View v) {
        checkOcServer();
    }


    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initServerPreFragment(Bundle savedInstanceState) {

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        boolean isUrlInputAllowed = getResources().getBoolean(R.bool.show_server_url_input);
        if (savedInstanceState == null) {
            if (mAccount != null) {
                mServerInfo.mBaseUrl = mAccountMgr.getUserData(mAccount, Constants.KEY_OC_BASE_URL);
                // TODO do next in a setter for mBaseUrl
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL);
                mServerInfo.mVersion = accountManager.getServerVersion(mAccount);
            } else {
                if (!webViewLoginMethod) {
                    mServerInfo.mBaseUrl = getString(R.string.server_url).trim();
                } else {
                    mServerInfo.mBaseUrl = getString(R.string.webview_login_url).trim();
                }
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL);
            }
        } else {
            mServerStatusText = savedInstanceState.getString(KEY_SERVER_STATUS_TEXT);
            mServerStatusIcon = savedInstanceState.getInt(KEY_SERVER_STATUS_ICON);

            mServerIsValid = savedInstanceState.getBoolean(KEY_SERVER_VALID);
            mServerIsChecked = savedInstanceState.getBoolean(KEY_SERVER_CHECKED);

            // TODO parcelable
            mServerInfo.mIsSslConn = savedInstanceState.getBoolean(KEY_IS_SSL_CONN);
            mServerInfo.mBaseUrl = savedInstanceState.getString(KEY_HOST_URL_TEXT);
            String ocVersion = savedInstanceState.getString(KEY_OC_VERSION);
            if (ocVersion != null) {
                mServerInfo.mVersion = new OwnCloudVersion(ocVersion);
            }
            mServerInfo.mAuthMethod = AuthenticationMethod.valueOf(
                    savedInstanceState.getString(KEY_SERVER_AUTH_METHOD));

        }

        if (!webViewLoginMethod) {
            /// step 2 - set properties of UI elements (text, visibility, enabled...)
            mHostUrlInput = findViewById(R.id.hostUrlInput);
            // Convert IDN to Unicode
            mHostUrlInput.setText(DisplayUtils.convertIdn(mServerInfo.mBaseUrl, false));
            if (mAction != ACTION_CREATE) {
                /// lock things that should not change
                mHostUrlInput.setEnabled(false);
                mHostUrlInput.setFocusable(false);
            }
            if (isUrlInputAllowed) {
                mRefreshButton = findViewById(R.id.embeddedRefreshButton);
            } else {
                findViewById(R.id.hostUrlFrame).setVisibility(View.GONE);
                mRefreshButton = findViewById(R.id.centeredRefreshButton);
            }
            showRefreshButton(mServerIsChecked && !mServerIsValid &&
                    mWaitingForOpId > Integer.MAX_VALUE);
            mServerStatusView = findViewById(R.id.server_status_text);
            showServerStatus();

            /// step 3 - bind some listeners and options
            mHostUrlInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            mHostUrlInput.setOnEditorActionListener(this);

            /// step 4 - create listeners that will be bound at onResume
            mHostUrlInputWatcher = new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {
                    if (mOkButton.isEnabled() &&
                            !mServerInfo.mBaseUrl.equals(
                                    AuthenticatorUrlUtils.normalizeUrl(s.toString(), mServerInfo.mIsSslConn))) {
                        mOkButton.setEnabled(false);
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // not used at the moment
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mAuthStatusIcon != 0) {
                        Log_OC.d(TAG, "onTextChanged: hiding authentication status");
                        mAuthStatusIcon = 0;
                        mAuthStatusText = EMPTY_STRING;
                        showAuthStatus();
                    }
                }
            };
        }
    }

    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initAuthorizationPreFragment(Bundle savedInstanceState) {

        /// step 0 - get UI elements in layout
        mUsernameInput = findViewById(R.id.account_username);
        mPasswordInput = findViewById(R.id.account_password);
        mAuthStatusView = findViewById(R.id.auth_status_text);

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        String presetUserName = null;
        boolean isPasswordExposed = false;
        if (savedInstanceState == null) {
            if (mAccount != null) {
                presetUserName = com.owncloud.android.lib.common.accounts.AccountUtils.getUsernameForAccount(mAccount);
            }
        } else {
            isPasswordExposed = savedInstanceState.getBoolean(KEY_PASSWORD_EXPOSED, false);
            mAuthStatusText = savedInstanceState.getString(KEY_AUTH_STATUS_TEXT);
            mAuthStatusIcon = savedInstanceState.getInt(KEY_AUTH_STATUS_ICON);
            mAuthToken = savedInstanceState.getString(KEY_AUTH_TOKEN);
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        if (presetUserName != null) {
            mUsernameInput.setText(presetUserName);
        }
        if (mAction != ACTION_CREATE) {
            mUsernameInput.setEnabled(false);
            mUsernameInput.setFocusable(false);
        }
        mPasswordInput.setText(EMPTY_STRING); // clean password to avoid social hacking
        if (isPasswordExposed) {
            showPassword();
        }
        showAuthStatus();
        mOkButton.setEnabled(mServerIsValid);


        /// step 3 - bind listeners
        // bindings for password input field
        mPasswordInput.setOnFocusChangeListener(this);
        mPasswordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPasswordInput.setOnEditorActionListener(this);
        mPasswordInput.setOnTouchListener(new RightDrawableOnTouchListener() {
            @Override
            public boolean onDrawableTouch(final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    AuthenticatorActivity.this.onViewPasswordClick();
                }
                return true;
            }
        });

    }

    /**
     * Saves relevant state before {@link #onPause()}
     *
     * See {@link super#onSaveInstanceState(Bundle)}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Log_OC.e(TAG, "onSaveInstanceState init" );
        super.onSaveInstanceState(outState);

        /// global state
        outState.putLong(KEY_WAITING_FOR_OP_ID, mWaitingForOpId);

        if (!webViewLoginMethod) {
            /// Server PRE-fragment state
            outState.putString(KEY_SERVER_STATUS_TEXT, mServerStatusText);
            outState.putInt(KEY_SERVER_STATUS_ICON, mServerStatusIcon);
            outState.putBoolean(KEY_SERVER_CHECKED, mServerIsChecked);
            outState.putBoolean(KEY_SERVER_VALID, mServerIsValid);

            /// Authentication PRE-fragment state
            outState.putBoolean(KEY_PASSWORD_EXPOSED, isPasswordVisible());
            outState.putInt(KEY_AUTH_STATUS_ICON, mAuthStatusIcon);
            outState.putString(KEY_AUTH_STATUS_TEXT, mAuthStatusText);
            outState.putString(KEY_AUTH_TOKEN, mAuthToken);
        }

        outState.putBoolean(KEY_IS_SSL_CONN, mServerInfo.mIsSslConn);
        outState.putString(KEY_HOST_URL_TEXT, mServerInfo.mBaseUrl);
        if (mServerInfo.mVersion != null) {
            outState.putString(KEY_OC_VERSION, mServerInfo.mVersion.getVersion());
        }
        outState.putString(KEY_SERVER_AUTH_METHOD, mServerInfo.mAuthMethod.name());

        /// authentication
        outState.putBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG, mIsFirstAuthAttempt);

        /// AsyncTask (User and password)
        if (!webViewLoginMethod) {
            outState.putString(KEY_USERNAME, mUsernameInput.getText().toString().trim());
            outState.putString(KEY_PASSWORD, mPasswordInput.getText().toString());
        }

        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, true);
        } else {
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, false);
        }
        mAsyncTask = null;

        //Log_OC.e(TAG, "onSaveInstanceState end" );
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mServerIsChecked = savedInstanceState.getBoolean(KEY_SERVER_CHECKED, false);

        // AsyncTask
        boolean inProgress = savedInstanceState.getBoolean(KEY_ASYNC_TASK_IN_PROGRESS);
        if (inProgress) {
            String username = savedInstanceState.getString(KEY_USERNAME);
            String password = savedInstanceState.getString(KEY_PASSWORD);

            OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password);
            accessRootFolder(credentials);
        }
    }

    /**
     * The redirection triggered by the OAuth authentication server as response to the
     * GET AUTHORIZATION request is caught here.
     *
     * To make this possible, this activity needs to be qualified with android:launchMode =
     * "singleTask" in the AndroidManifest.xml file.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log_OC.d(TAG, "onNewIntent()");

        if (intent.getBooleanExtra(FirstRunActivity.EXTRA_EXIT, false)) {
            super.finish();
        }

        // Passcode
        PassCodeManager passCodeManager = new PassCodeManager(preferences);
        passCodeManager.onActivityStarted(this);

        Uri data = intent.getData();

        if (data != null && data.toString().startsWith(getString(R.string.login_data_own_scheme))) {
            parseAndLoginFromWebView(data.toString());
        }

        if (intent.getBooleanExtra(EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)) {
            webViewLoginMethod = true;
            setContentView(R.layout.account_setup_webview);
            mLoginWebView = findViewById(R.id.login_webview);
            initWebViewLogin(getString(R.string.provider_registration_server), false, true);
        }
    }


    /**
     * The redirection triggered by the OAuth authentication server as response to the
     * GET AUTHORIZATION, and deferred in {@link #onNewIntent(Intent)}, is processed here.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (!webViewLoginMethod) {
            // bound here to avoid spurious changes triggered by Android on device rotations
            mHostUrlInput.setOnFocusChangeListener(this);
            mHostUrlInput.addTextChangedListener(mHostUrlInputWatcher);

            String dataString = getIntent().getDataString();
            if (dataString != null) {
                try {
                    populateLoginFields(dataString);
                } catch (IllegalArgumentException e) {
                    DisplayUtils.showSnackMessage(findViewById(R.id.scroll), R.string.auth_illegal_login_used);
                    Log_OC.e(TAG, "Illegal login data URL used, no Login pre-fill!", e);
                }
            }
        }

        // bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        if (!bindService(new Intent(this, OperationsService.class),
                mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            DisplayUtils.showSnackMessage(findViewById(R.id.scroll), R.string.error_cant_bind_to_operations_service);
            finish();
        }

        if (mOperationsServiceBinder != null) {
            doOnResumeAndBound();
        }
    }


    @Override
    protected void onPause() {
        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }

        if (!webViewLoginMethod) {
            mHostUrlInput.removeTextChangedListener(mHostUrlInputWatcher);
            mHostUrlInput.setOnFocusChangeListener(null);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        mHostUrlInputWatcher = null;

        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        if (webViewLoginMethod) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }

        super.onDestroy();
    }

    /**
     * Handles the change of focus on the text inputs for the server URL and the password
     */
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.hostUrlInput) {
            if (!hasFocus) {
                onUrlInputFocusLost();
            } else {
                showRefreshButton(false);
            }

        } else if (view.getId() == R.id.account_password) {
            onPasswordFocusChanged(hasFocus);
        }
    }


    /**
     * Handles changes in focus on the text input for the server URL.
     *
     * IMPORTANT ENTRY POINT 2: When (!hasFocus), user wrote the server URL and changed to
     * other field. The operation to check the existence of the server in the entered URL is
     * started.
     *
     * When hasFocus:    user 'comes back' to write again the server URL.
     */
    private void onUrlInputFocusLost() {
        if (!mServerInfo.mBaseUrl.equals(
                AuthenticatorUrlUtils.normalizeUrl(mHostUrlInput.getText().toString(), mServerInfo.mIsSslConn))) {
            // check server again only if the user changed something in the field
            checkOcServer();
        } else {
            mOkButton.setEnabled(mServerIsValid);
            showRefreshButton(!mServerIsValid);
        }
    }


    private void checkOcServer() {
        String uri;
        if (mHostUrlInput != null && !mHostUrlInput.getText().toString().isEmpty()) {
            uri = mHostUrlInput.getText().toString().trim();
            mOkButton.setEnabled(false);
            showRefreshButton(false);
        } else {
            uri = mServerInfo.mBaseUrl;
        }

        mServerIsValid = false;
        mServerIsChecked = false;
        mServerInfo = new GetServerInfoOperation.ServerInfo();

        if (uri.length() != 0) {
            if (mHostUrlInput != null) {
                uri = AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles(uri);
                mHostUrlInput.setText(uri);
            }

            // Handle internationalized domain names
            try {
                uri = DisplayUtils.convertIdn(uri, true);
            } catch (IllegalArgumentException ex) {
                // Let Owncloud library check the error of the malformed URI
                Log_OC.e(TAG, "Error converting internationalized domain name " + uri, ex);
            }

            if (mHostUrlInput != null) {
                mServerStatusText = getResources().getString(R.string.auth_testing_connection);
                mServerStatusIcon = R.drawable.progress_small;
                showServerStatus();
            }

            Intent getServerInfoIntent = new Intent();
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
            getServerInfoIntent.putExtra(OperationsService.EXTRA_SERVER_URL,
                    AuthenticatorUrlUtils.normalizeUrlSuffix(uri));

            if (mOperationsServiceBinder != null) {
                mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getServerInfoIntent);
            } else {
                Log_OC.e(TAG, "Server check tried with OperationService unbound!");
            }

        } else {
            mServerStatusText = EMPTY_STRING;
            mServerStatusIcon = 0;
            if (!webViewLoginMethod) {
                showServerStatus();
            }
        }
    }


    /**
     * Handles changes in focus on the text input for the password (basic authorization).
     *
     * When (hasFocus), the button to toggle password visibility is shown.
     *
     * When (!hasFocus), the button is made invisible and the password is hidden.
     *
     * @param hasFocus 'True' if focus is received, 'false' if is lost
     */
    private void onPasswordFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            showViewPasswordButton();
        } else {
            hidePassword();
            hidePasswordButton();
        }
    }

    private void showViewPasswordButton() {
        int drawable = R.drawable.ic_view;
        if (isPasswordVisible()) {
            drawable = R.drawable.ic_hide;
        }
        mPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
    }

    private boolean isPasswordVisible() {
        return ((mPasswordInput.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) ==
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    private void hidePasswordButton() {
        mPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    private void showPassword() {
        mPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD |
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        showViewPasswordButton();
    }

    private void hidePassword() {
        mPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD |
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        showViewPasswordButton();
    }

    /**
     * Checks the credentials of the user in the root of the ownCloud server
     * before creating a new local account.
     *
     * For basic authorization, a check of existence of the root folder is
     * performed.
     *
     * For OAuth, starts the flow to get an access token; the credentials test
     * is postponed until it is available.
     *
     * IMPORTANT ENTRY POINT 4
     */
    public void onOkClick() {
        // this check should be unnecessary
        if (mServerInfo.mVersion == null ||
            !mServerInfo.mVersion.isVersionValid() ||
            TextUtils.isEmpty(mServerInfo.mBaseUrl)) {
            mServerStatusIcon = R.drawable.ic_alert;
            mServerStatusText = getResources().getString(R.string.auth_wtf_reenter_URL);
            showServerStatus();
            mOkButton.setEnabled(false);
            return;
        }

        checkBasicAuthorization(null, null);
    }


    /**
     * Tests the credentials entered by the user performing a check of existence on
     * the root folder of the ownCloud server.
     */
    private void checkBasicAuthorization(@Nullable String webViewUsername, @Nullable String webViewPassword) {
        /// get basic credentials entered by user
        String username;
        String password;
        if (!webViewLoginMethod) {
            username = mUsernameInput.getText().toString().trim();
            password = mPasswordInput.getText().toString();
        } else {
            username = webViewUsername;
            password = webViewPassword;
        }

        /// be gentle with the user
        IndeterminateProgressDialog dialog = IndeterminateProgressDialog.newInstance(R.string.auth_trying_to_login,
                                                                                     true);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, WAIT_DIALOG_TAG);
        ft.commitAllowingStateLoss();

        /// validate credentials accessing the root folder
        OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password);
        accessRootFolder(credentials);
    }

    private void accessRootFolder(OwnCloudCredentials credentials) {
        mAsyncTask = new AuthenticatorAsyncTask(this);
        Object[] params = {mServerInfo.mBaseUrl, credentials};
        mAsyncTask.execute(params);
    }

    /**
     * Callback method invoked when a RemoteOperation executed by this Activity finishes.
     *
     * Dispatches the operation flow to the right method.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof GetServerInfoOperation) {
            if (operation.hashCode() == mWaitingForOpId) {
                onGetServerInfoFinish(result);
            }   // else nothing ; only the last check operation is considered;
            // multiple can be started if the user amends a URL quickly

        } else if (operation instanceof GetUserInfoRemoteOperation) {
            onGetUserNameFinish(result);
        }
    }

    private void onGetUserNameFinish(RemoteOperationResult result) {
        mWaitingForOpId = Long.MAX_VALUE;
        if (result.isSuccess()) {
            boolean success = false;
            String username;
            if (result.getData().get(0) instanceof UserInfo) {
                username = ((UserInfo) result.getData().get(0)).getDisplayName();
            } else {
                username = (String) result.getData().get(0);
            }

            if (mAction == ACTION_CREATE) {
                if (!webViewLoginMethod) {
                    mUsernameInput.setText(username);
                }
                success = createAccount(result);
            } else {

                if (!webViewLoginMethod && !mUsernameInput.getText().toString().trim().equals(username)) {
                    // fail - not a new account, but an existing one; disallow
                    result = new RemoteOperationResult(ResultCode.ACCOUNT_NOT_THE_SAME);
                    mAuthToken = EMPTY_STRING;
                    updateAuthStatusIconAndText(result);
                    showAuthStatus();
                    Log_OC.d(TAG, result.getLogMessage());
                } else {
                    try {
                        updateAccountAuthentication();
                        success = true;

                    } catch (AccountNotFoundException e) {
                        Log_OC.e(TAG, "Account " + mAccount + " was removed!", e);
                        DisplayUtils.showSnackMessage(findViewById(R.id.scroll), R.string.auth_account_does_not_exist);
                        finish();
                    }
                }
            }

            if (success) {
                finish();
            }
        } else {
            if (!webViewLoginMethod) {
                int statusText = result.getCode() == ResultCode.MAINTENANCE_MODE ? R.string.maintenance_mode : R.string.auth_fail_get_user_name;
                updateStatusIconFailUserName(statusText);
                showAuthStatus();
            }
            Log_OC.e(TAG, "Access to user name failed: " + result.getLogMessage());
        }

    }

    /**
     * Processes the result of the server check performed when the user finishes the enter of the
     * server URL.
     *
     * @param result Result of the check.
     */
    private void onGetServerInfoFinish(RemoteOperationResult result) {
        /// update activity state
        mServerIsChecked = true;
        mWaitingForOpId = Long.MAX_VALUE;

        // update server status, but don't show it yet
        if (!webViewLoginMethod) {
            updateServerStatusIconAndText(result);
        }

        if (result.isSuccess()) {
            /// SUCCESS means:
            //      1. connection succeeded, and we know if it's SSL or not
            //      2. server is installed
            //      3. we got the server version
            //      4. we got the authentication method required by the server
            mServerInfo = (GetServerInfoOperation.ServerInfo) (result.getData().get(0));

            // show outdated warning
            if (getResources().getBoolean(R.bool.show_outdated_server_warning) &&
                MainApp.OUTDATED_SERVER_VERSION.compareTo(mServerInfo.mVersion) >= 0 &&
                !mServerInfo.hasExtendedSupport) {
                DisplayUtils.showServerOutdatedSnackbar(this, Snackbar.LENGTH_INDEFINITE);
            }

            webViewLoginMethod = mServerInfo.mVersion.isWebLoginSupported() && !forceOldLoginMethod;

            if (webViewUser != null && !webViewUser.isEmpty() &&
                    webViewPassword != null && !webViewPassword.isEmpty()) {
                checkBasicAuthorization(webViewUser, webViewPassword);
            } else if (webViewLoginMethod) {
                // hide old login
                setOldLoginVisibility(View.GONE);

                setContentView(R.layout.account_setup_webview);
                mLoginWebView = findViewById(R.id.login_webview);
                initWebViewLogin(mServerInfo.mBaseUrl + WEB_LOGIN, true, false);
            } else {
                // show old login
                setOldLoginVisibility(View.VISIBLE);
            }

            if (!authSupported(mServerInfo.mAuthMethod)) {

                if (!webViewLoginMethod) {
                    // overrides updateServerStatusIconAndText()
                    updateServerStatusIconNoRegularAuth();
                }
                mServerIsValid = false;

            } else {
                mServerIsValid = true;
            }

        } else {
            mServerIsValid = false;
        }

        // refresh UI
        if (!webViewLoginMethod) {
            showRefreshButton(!mServerIsValid);
            showServerStatus();
            mOkButton.setEnabled(mServerIsValid);
        }

        if (!mServerIsValid) {
            // hide old login
            setOldLoginVisibility(View.GONE);
        }

        /// very special case (TODO: move to a common place for all the remote operations)
        if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result);
        }
    }

    private void setOldLoginVisibility(int visible) {
        mOkButton.setVisibility(visible);
        mUsernameInputLayout.setVisibility(visible);
        mPasswordInputLayout.setVisibility(visible);
    }

    private boolean authSupported(AuthenticationMethod authMethod) {
        return AuthenticationMethod.BASIC_HTTP_AUTH.equals(authMethod);
    }

    /**
     * Chooses the right icon and text to show to the user for the received operation result.
     *
     * @param result Result of a remote operation performed in this activity
     */
    private void updateServerStatusIconAndText(RemoteOperationResult result) {
        mServerStatusIcon = R.drawable.ic_alert;    // the most common case in the switch below

        switch (result.getCode()) {
            case OK_SSL:
                mServerStatusIcon = R.drawable.ic_lock_white;
                mServerStatusText = getResources().getString(R.string.auth_secure_connection);
                break;

            case OK_NO_SSL:
            case OK:
                if (mHostUrlInput.getText().toString().trim().toLowerCase(Locale.ROOT).startsWith(HTTP_PROTOCOL)) {
                    mServerStatusText = getResources().getString(R.string.auth_connection_established);
                    mServerStatusIcon = R.drawable.ic_ok;
                } else {
                    mServerStatusText = getResources().getString(R.string.auth_nossl_plain_ok_title);
                    mServerStatusIcon = R.drawable.ic_lock_open_white;
                }
                break;

            case NO_NETWORK_CONNECTION:
                mServerStatusIcon = R.drawable.no_network;
                mServerStatusText = getResources().getString(R.string.auth_no_net_conn_title);
                break;

            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                mServerStatusText = getResources().getString(R.string.auth_ssl_unverified_server_title);
                break;
            case BAD_OC_VERSION:
                mServerStatusText = getResources().getString(R.string.auth_bad_oc_version_title);
                break;
            case WRONG_CONNECTION:
                mServerStatusText = getResources().getString(R.string.auth_wrong_connection_title);
                break;
            case TIMEOUT:
                mServerStatusText = getResources().getString(R.string.auth_timeout_title);
                break;
            case INCORRECT_ADDRESS:
                mServerStatusText = getResources().getString(R.string.auth_incorrect_address_title);
                break;
            case SSL_ERROR:
                mServerStatusText = getResources().getString(R.string.auth_ssl_general_error_title);
                break;
            case UNAUTHORIZED:
                mServerStatusText = getResources().getString(R.string.auth_unauthorized);
                break;
            case HOST_NOT_AVAILABLE:
                mServerStatusText = getResources().getString(R.string.auth_unknown_host_title);
                break;
            case INSTANCE_NOT_CONFIGURED:
                mServerStatusText = getResources().getString(R.string.auth_not_configured_title);
                break;
            case FILE_NOT_FOUND:
                mServerStatusText = getResources().getString(R.string.auth_incorrect_path_title);
                break;
            case OAUTH2_ERROR:
                mServerStatusText = getResources().getString(R.string.auth_oauth_error);
                break;
            case OAUTH2_ERROR_ACCESS_DENIED:
                mServerStatusText = getResources().getString(R.string.auth_oauth_error_access_denied);
                break;
            case UNHANDLED_HTTP_CODE:
                mServerStatusText = getResources().getString(R.string.auth_unknown_error_http_title);
                break;
            case UNKNOWN_ERROR:
                if (result.getException() != null &&
                        !TextUtils.isEmpty(result.getException().getMessage())) {
                    mServerStatusText = getResources().getString(
                            R.string.auth_unknown_error_exception_title,
                            result.getException().getMessage()
                    );
                } else {
                    mServerStatusText = getResources().getString(R.string.auth_unknown_error_title);
                }
                break;
            case OK_REDIRECT_TO_NON_SECURE_CONNECTION:
                mServerStatusIcon = R.drawable.ic_lock_open_white;
                mServerStatusText = getResources().getString(R.string.auth_redirect_non_secure_connection_title);
                break;
            case MAINTENANCE_MODE:
                mServerStatusText = getResources().getString(R.string.maintenance_mode);
                break;
            case UNTRUSTED_DOMAIN:
                mServerStatusText = getResources().getString(R.string.untrusted_domain);
                break;
            default:
                mServerStatusText = EMPTY_STRING;
                mServerStatusIcon = 0;
                break;
        }
    }


    /**
     * Chooses the right icon and text to show to the user for the received operation result.
     *
     * @param result Result of a remote operation performed in this activity
     */
    private void updateAuthStatusIconAndText(RemoteOperationResult result) {
        mAuthStatusIcon = R.drawable.ic_alert;    // the most common case in the switch below

        switch (result.getCode()) {
            case OK_SSL:
                mAuthStatusIcon = R.drawable.ic_lock_white;
                mAuthStatusText = getResources().getString(R.string.auth_secure_connection);
                break;

            case OK_NO_SSL:
            case OK:
                if (mHostUrlInput.getText().toString().trim().toLowerCase(Locale.ROOT).startsWith(HTTP_PROTOCOL)) {
                    mAuthStatusText = getResources().getString(R.string.auth_connection_established);
                    mAuthStatusIcon = R.drawable.ic_ok;
                } else {
                    mAuthStatusText = getResources().getString(R.string.auth_nossl_plain_ok_title);
                    mAuthStatusIcon = R.drawable.ic_lock_open_white;
                }
                break;

            case NO_NETWORK_CONNECTION:
                mAuthStatusIcon = R.drawable.no_network;
                mAuthStatusText = getResources().getString(R.string.auth_no_net_conn_title);
                break;

            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                mAuthStatusText = getResources().getString(R.string.auth_ssl_unverified_server_title);
                break;
            case TIMEOUT:
                mAuthStatusText = getResources().getString(R.string.auth_timeout_title);
                break;
            case HOST_NOT_AVAILABLE:
                mAuthStatusText = getResources().getString(R.string.auth_unknown_host_title);
                break;
            case UNHANDLED_HTTP_CODE:
            default:
                mAuthStatusText = ErrorMessageAdapter.getErrorCauseMessage(result, null, getResources());
        }
    }

    private void updateStatusIconFailUserName(int failedStatusText) {
        mAuthStatusIcon = R.drawable.ic_alert;
        mAuthStatusText = getResources().getString(failedStatusText);
    }

    private void updateServerStatusIconNoRegularAuth() {
        mServerStatusIcon = R.drawable.ic_alert;
        mServerStatusText = getResources().getString(R.string.auth_can_not_auth_against_server);
    }

    /**
     * Processes the result of the access check performed to try the user credentials.
     *
     * Creates a new account through the AccountManager.
     *
     * @param result Result of the operation.
     */
    @Override
    public void onAuthenticatorTaskCallback(RemoteOperationResult result) {
        mWaitingForOpId = Long.MAX_VALUE;
        dismissDialog(WAIT_DIALOG_TAG);
        mAsyncTask = null;

        if (result.isSuccess()) {
            Log_OC.d(TAG, "Successful access - time to save the account");

            boolean success = false;

            if (mAction == ACTION_CREATE) {
                success = createAccount(result);

            } else {
                try {
                    updateAccountAuthentication();
                    success = true;

                } catch (AccountNotFoundException e) {
                    Log_OC.e(TAG, "Account " + mAccount + " was removed!", e);
                    DisplayUtils.showSnackMessage(findViewById(R.id.scroll), R.string.auth_account_does_not_exist);
                    finish();
                }
            }

            // Reset webView
            webViewPassword = null;
            webViewUser = null;
            forceOldLoginMethod = false;
            deleteCookies();

            if (success) {
                finish();

                accountManager.setCurrentOwnCloudAccount(mAccount.name);

                Intent i = new Intent(this, FileDisplayActivity.class);
                i.setAction(FileDisplayActivity.RESTART);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

            } else {
                // init webView again
                if (mLoginWebView != null) {
                    mLoginWebView.setVisibility(View.GONE);
                }
                setContentView(R.layout.account_setup);

                initOverallUi();

                CustomEditText serverAddressField = findViewById(R.id.hostUrlInput);
                serverAddressField.setText(mServerInfo.mBaseUrl);

                findViewById(R.id.server_status_text).setVisibility(View.GONE);
                mAuthStatusView = findViewById(R.id.auth_status_text);

                showAuthStatus();
            }

        } else if (result.isServerFail() || result.isException()) {
            /// server errors or exceptions in authorization take to requiring a new check of
            /// the server
            mServerIsChecked = true;
            mServerIsValid = false;
            mServerInfo = new GetServerInfoOperation.ServerInfo();

            // update status icon and text
            updateServerStatusIconAndText(result);
            showServerStatus();
            mAuthStatusIcon = 0;
            mAuthStatusText = EMPTY_STRING;
            if (!webViewLoginMethod) {
                showAuthStatus();

                // update input controls state
                showRefreshButton(true);
                mOkButton.setEnabled(false);
            }

            // very special case (TODO: move to a common place for all the remote operations)
            if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
                showUntrustedCertDialog(result);
            }

        } else {    // authorization fail due to client side - probably wrong credentials
            if (webViewLoginMethod) {
                mLoginWebView = findViewById(R.id.login_webview);

                if (mLoginWebView != null) {
                    initWebViewLogin(mServerInfo.mBaseUrl + WEB_LOGIN, true, false);
                    DisplayUtils.showSnackMessage(this, mLoginWebView, R.string.auth_access_failed,
                                                  result.getLogMessage());
                } else {
                    DisplayUtils.showSnackMessage(this, R.string.auth_access_failed, result.getLogMessage());

                    // init webView again
                    if (mLoginWebView != null) {
                        mLoginWebView.setVisibility(View.GONE);
                    }
                    setContentView(R.layout.account_setup);

                    initOverallUi();

                    CustomEditText serverAddressField = findViewById(R.id.hostUrlInput);
                    serverAddressField.setText(mServerInfo.mBaseUrl);

                    findViewById(R.id.server_status_text).setVisibility(View.GONE);
                    mAuthStatusView = findViewById(R.id.auth_status_text);

                    showAuthStatus();
                }
            } else {
                updateAuthStatusIconAndText(result);
                showAuthStatus();
            }
            // reset webview
            webViewPassword = null;
            webViewUser = null;
            deleteCookies();

            Log_OC.d(TAG, "Access failed: " + result.getLogMessage());
        }
    }

    /**
     * Updates the authentication token.
     *
     * Sets the proper response so that the AccountAuthenticator that started this activity
     * saves a new authorization token for mAccount.
     *
     * Kills the session kept by OwnCloudClientManager so that a new one will created with
     * the new credentials when needed.
     */
    private void updateAccountAuthentication() throws AccountNotFoundException {
        Bundle response = new Bundle();
        response.putString(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
        response.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccount.type);

        if (webViewLoginMethod) {
            response.putString(AccountManager.KEY_AUTHTOKEN, webViewPassword);
            mAccountMgr.setPassword(mAccount, webViewPassword);
        } else {
            response.putString(AccountManager.KEY_AUTHTOKEN, mPasswordInput.getText().toString());
            mAccountMgr.setPassword(mAccount, mPasswordInput.getText().toString());
        }

        // remove managed clients for this account to enforce creation with fresh credentials
        OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, this);
        OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount);

        setAccountAuthenticatorResult(response);
        final Intent intent = new Intent();
        intent.putExtras(response);
        setResult(RESULT_OK, intent);

    }

    /**
     * Creates a new account through the Account Authenticator that started this activity.
     *
     * This makes the account permanent.
     *
     * TODO Decide how to name the OAuth accounts
     */
    @SuppressFBWarnings("DMI")
    @SuppressLint("TrulyRandom")
    protected boolean createAccount(RemoteOperationResult authResult) {
        String accountType = MainApp.getAccountType(this);

        // create and save new ownCloud account
        String lastPermanentLocation = authResult.getLastPermanentLocation();
        if (lastPermanentLocation != null) {
            mServerInfo.mBaseUrl = AuthenticatorUrlUtils.trimWebdavSuffix(lastPermanentLocation);
        }

        Uri uri = Uri.parse(mServerInfo.mBaseUrl);
        // used for authenticate on every login/network connection, determined by first login (weblogin/old login)
        // can be anything: email, name, name with whitespaces
        String loginName;
        if (!webViewLoginMethod) {
            loginName = mUsernameInput.getText().toString().trim();
        } else {
            loginName = webViewUser;
        }

        String accountName = com.owncloud.android.lib.common.accounts.AccountUtils.buildAccountName(uri, loginName);
        Account newAccount = new Account(accountName, accountType);
        if (accountManager.exists(newAccount)) {
            // fail - not a new account, but an existing one; disallow
            RemoteOperationResult result = new RemoteOperationResult(ResultCode.ACCOUNT_NOT_NEW);

            updateAuthStatusIconAndText(result);
            showAuthStatus();

            Log_OC.d(TAG, result.getLogMessage());
            return false;

        } else {
            mAccount = newAccount;

            if (webViewLoginMethod) {
                mAccountMgr.addAccountExplicitly(mAccount, webViewPassword, null);
            } else {
                mAccountMgr.addAccountExplicitly(mAccount, mPasswordInput.getText().toString(), null);
            }

            /// add the new account as default in preferences, if there is none already
            Account defaultAccount = accountManager.getCurrentAccount();
            if (defaultAccount == null) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString("select_oc_account", accountName);
                editor.apply();
            }

            /// prepare result to return to the Authenticator
            //  TODO check again what the Authenticator makes with it; probably has the same
            //  effect as addAccountExplicitly, but it's not well done
            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
            intent.putExtra(AccountManager.KEY_USERDATA, loginName);

            /// add user data to the new account; TODO probably can be done in the last parameter
            //      addAccountExplicitly, or in KEY_USERDATA
            mAccountMgr.setUserData(mAccount, Constants.KEY_OC_VERSION, mServerInfo.mVersion.getVersion());
            mAccountMgr.setUserData(mAccount, Constants.KEY_OC_BASE_URL, mServerInfo.mBaseUrl);

            ArrayList<Object> authResultData = authResult.getData();
            if (authResultData == null || authResultData.size() == 0) {
                Log_OC.e(this, "Could not read user data!");
                return false;
            }

            UserInfo userInfo = (UserInfo) authResultData.get(0);
            mAccountMgr.setUserData(mAccount, Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
            mAccountMgr.setUserData(mAccount, Constants.KEY_USER_ID, userInfo.getId());
            mAccountMgr.setUserData(mAccount, Constants.KEY_OC_ACCOUNT_VERSION,
                                    Integer.toString(AccountUtils.ACCOUNT_VERSION_WITH_PROPER_ID));


            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);

            return true;
        }
    }

    public void onScan() {
        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startQRScanner();
        } else {
            PermissionUtil.requestCameraPermission(this);
        }
    }

    private void startQRScanner() {
        Intent i = new Intent(this, QrCodeActivity.class);
        startActivityForResult(i, REQUEST_CODE_QR_SCAN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.PERMISSIONS_CAMERA: {
                // If request is cancelled, result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startQRScanner();
                } else {
                    // permission denied
                    return;
                }
                return;
            }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
    /**
     * Updates the content and visibility state of the icon and text associated
     * to the last check on the ownCloud server.
     */
    private void showServerStatus() {
        if (mServerStatusIcon == NO_ICON && EMPTY_STRING.equals(mServerStatusText)) {
            mServerStatusView.setVisibility(View.INVISIBLE);
        } else {
            mServerStatusView.setText(mServerStatusText);
            mServerStatusView.setCompoundDrawablesWithIntrinsicBounds(mServerStatusIcon, 0, 0, 0);
            mServerStatusView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the content and visibility state of the icon and text associated
     * to the interactions with the OAuth authorization server.
     */
    private void showAuthStatus() {
        if (mAuthStatusIcon == NO_ICON && EMPTY_STRING.equals(mAuthStatusText)) {
            mAuthStatusView.setVisibility(View.INVISIBLE);
        } else {
            mAuthStatusView.setText(mAuthStatusText);
            mAuthStatusView.setCompoundDrawablesWithIntrinsicBounds(mAuthStatusIcon, 0, 0, 0);
            mAuthStatusView.setVisibility(View.VISIBLE);
        }
    }

    private void showRefreshButton(boolean show) {
        if (webViewLoginMethod && mRefreshButton != null) {
            if (show) {
                mRefreshButton.setVisibility(View.VISIBLE);
            } else {
                mRefreshButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Called when the eye icon in the password field is clicked.
     *
     * Toggles the visibility of the password in the field.
     */
    public void onViewPasswordClick() {
        int selectionStart = mPasswordInput.getSelectionStart();
        int selectionEnd = mPasswordInput.getSelectionEnd();
        if (isPasswordVisible()) {
            hidePassword();
        } else {
            showPassword();
        }
        mPasswordInput.setSelection(selectionStart, selectionEnd);
    }

    /**
     * Called when the 'action' button in an IME is pressed ('enter' in software keyboard).
     *
     * Used to trigger the authentication check when the user presses 'enter' after writing the
     * password, or to throw the server test when the only field on screen is the URL input field.
     */
    @Override
    public boolean onEditorAction(TextView inputField, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && inputField != null &&
                inputField.equals(mPasswordInput)) {
            if (mOkButton.isEnabled()) {
                mOkButton.performClick();
            }

        } else if ((actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL)
                && inputField != null && inputField.equals(mHostUrlInput)) {
            checkOcServer();
        }
        return false;   // always return false to grant that the software keyboard is hidden anyway
    }


    private abstract static class RightDrawableOnTouchListener implements OnTouchListener {

        private static final int RIGHT_DRAWABLE_COMPOUND_DRAWABLES_LENGTH = 2;

        private int fuzz = 75;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Drawable rightDrawable = null;
            if (view instanceof TextView) {
                Drawable[] drawables = ((TextView) view).getCompoundDrawables();
                if (drawables.length > RIGHT_DRAWABLE_COMPOUND_DRAWABLES_LENGTH) {
                    rightDrawable = drawables[2];
                }
            }
            if (rightDrawable != null) {
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                final Rect bounds = rightDrawable.getBounds();
                if (x >= (view.getRight() - bounds.width() - fuzz) &&
                        x <= (view.getRight() - view.getPaddingRight() + fuzz) &&
                        y >= (view.getPaddingTop() - fuzz) &&
                        y <= (view.getHeight() - view.getPaddingBottom()) + fuzz) {

                    return onDrawableTouch(event);
                }
            }
            return false;
        }

        public abstract boolean onDrawableTouch(final MotionEvent event);
    }

    /**
     * Show untrusted cert dialog
     */
    public void showUntrustedCertDialog(X509Certificate x509Certificate, SslError error, SslErrorHandler handler) {
        // Show a dialog with the certificate info
        SslUntrustedCertDialog dialog;
        if (x509Certificate == null) {
            dialog = SslUntrustedCertDialog.newInstanceForEmptySslError(error, handler);
        } else {
            dialog = SslUntrustedCertDialog.newInstanceForFullSslError(x509Certificate, error, handler);
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG);
    }


    /**
     * Show untrusted cert dialog
     */
    private void showUntrustedCertDialog(RemoteOperationResult result) {
        // Show a dialog with the certificate info
        SslUntrustedCertDialog dialog = SslUntrustedCertDialog.
                newInstanceForFullSslError((CertificateCombinedException) result.getException());
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG);

    }

    private void doOnResumeAndBound() {
        //Log_OC.e(TAG, "registering to listen for operation callbacks" );
        mOperationsServiceBinder.addOperationListener(this, mHandler);
        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int) mWaitingForOpId, this);
        }

        if (!webViewLoginMethod && !TextUtils.isEmpty(mHostUrlInput.getText()) && !mServerIsChecked) {
            checkOcServer();
        }
    }


    private void dismissDialog(String dialogTag) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(dialogTag);
        if (frag instanceof DialogFragment) {
            DialogFragment dialog = (DialogFragment) frag;

            try {
                dialog.dismiss();
            } catch (IllegalStateException e) {
                Log_OC.e(TAG, e.getMessage());
                dialog.dismissAllowingStateLoss();
            }
        }
    }

    /**
     * Implements callback methods for service binding.
     */
    private class OperationsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(
                    new ComponentName(AuthenticatorActivity.this, OperationsService.class)
            )) {
                mOperationsServiceBinder = (OperationsServiceBinder) service;

                Uri data = getIntent().getData();
                if (data != null && data.toString().startsWith(getString(R.string.login_data_own_scheme))) {
                    String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
                    LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, data.toString());

                    if (loginUrlInfo != null) {
                        try {
                            mServerInfo.mBaseUrl = AuthenticatorUrlUtils.normalizeUrlSuffix(loginUrlInfo.serverAddress);
                            webViewUser = loginUrlInfo.username;
                            webViewPassword = loginUrlInfo.password;
                            doOnResumeAndBound();
                        } catch (Exception e) {
                            mServerStatusIcon = R.drawable.ic_alert;
                            mServerStatusText = "QR Code could not be read!";
                            showServerStatus();
                        }
                    }
                } else {
                    doOnResumeAndBound();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(
                    new ComponentName(AuthenticatorActivity.this, OperationsService.class)
            )) {
                Log_OC.e(TAG, "Operations service crashed");
                mOperationsServiceBinder = null;
            }
        }
    }

    /**
     * Create and show dialog for request authentication to the user
     *
     * @param webView Web view to embed into the authentication dialog.
     * @param handler Object responsible for catching and recovering HTTP authentication fails.
     */
    public void createAuthenticationDialog(WebView webView, HttpAuthHandler handler) {

        // Show a dialog with the certificate info
        CredentialsDialogFragment dialog = CredentialsDialogFragment.newInstanceForCredentials(webView, handler);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog.setCancelable(false);
        dialog.show(ft, CREDENTIALS_DIALOG_TAG);

        if (!mIsFirstAuthAttempt) {
            DisplayUtils.showSnackMessage(this, R.string.saml_authentication_wrong_pass);
        } else {
            mIsFirstAuthAttempt = false;
        }
    }

    /**
     * For retrieving the clicking on authentication cancel button.
     */
    public void doNegativeAuthenticationDialogClick() {
        mIsFirstAuthAttempt = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null) {
                return;
            }

            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");

            if (result == null || !result.startsWith(getString(R.string.login_data_own_scheme))) {
                mServerStatusIcon = R.drawable.ic_alert;
                mServerStatusText = "QR Code could not be read!";
                showServerStatus();
                return;
            }

            parseAndLoginFromWebView(result);
        }
    }

    /**
     * Obtain the X509Certificate from SslError
     *
     * @param error SslError
     * @return X509Certificate from error
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

    /**
     * Called from SslValidatorDialog when a new server certificate was correctly saved.
     */
    public void onSavedCertificate() {
        checkOcServer();
    }

    /**
     * Called from SslValidatorDialog when a new server certificate could not be saved when the user requested it.
     */
    @Override
    public void onFailedSavingCertificate() {
        DisplayUtils.showSnackMessage(this, R.string.ssl_validator_not_saved);
    }
}
