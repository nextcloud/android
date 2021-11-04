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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.onboarding.OnboardingService;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.AccountSetupBinding;
import com.owncloud.android.databinding.AccountSetupWebviewBinding;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
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
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.DetectAuthenticationMethodOperation.AuthenticationMethod;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import de.cotech.hw.fido.WebViewFidoBridge;
import de.cotech.hw.fido.ui.FidoDialogOptions;
import de.cotech.hw.fido2.WebViewWebauthnBridge;
import de.cotech.hw.fido2.ui.WebauthnDialogOptions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This Activity is used to add an ownCloud account to the App
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
    implements OnRemoteOperationListener, OnEditorActionListener, OnSslUntrustedCertListener,
    AuthenticatorAsyncTask.OnAuthenticatorTaskListener, Injectable {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_USE_PROVIDER_AS_WEBLOGIN = "USE_PROVIDER_AS_WEBLOGIN";

    private static final String KEY_HOST_URL_TEXT = "HOST_URL_TEXT";
    private static final String KEY_OC_VERSION = "OC_VERSION";
    private static final String KEY_SERVER_STATUS_TEXT = "SERVER_STATUS_TEXT";
    private static final String KEY_SERVER_STATUS_ICON = "SERVER_STATUS_ICON";
    private static final String KEY_IS_SSL_CONN = "IS_SSL_CONN";
    private static final String KEY_AUTH_STATUS_TEXT = "AUTH_STATUS_TEXT";
    private static final String KEY_AUTH_STATUS_ICON = "AUTH_STATUS_ICON";
    private static final String KEY_SERVER_AUTH_METHOD = "SERVER_AUTH_METHOD";
    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String KEY_ONLY_ADD = "onlyAdd";

    public static final byte ACTION_CREATE = 0;
    public static final byte ACTION_UPDATE_EXPIRED_TOKEN = 2;       // detected by the app

    private static final String UNTRUSTED_CERT_DIALOG_TAG = "UNTRUSTED_CERT_DIALOG";
    private static final String WAIT_DIALOG_TAG = "WAIT_DIALOG";
    private static final String KEY_AUTH_IS_FIRST_ATTEMPT_TAG = "KEY_AUTH_IS_FIRST_ATTEMPT";

    private static final String KEY_USERNAME = "USERNAME";
    private static final String KEY_PASSWORD = "PASSWORD";
    private static final String KEY_ASYNC_TASK_IN_PROGRESS = "AUTH_IN_PROGRESS";
    public static final String WEB_LOGIN = "/index.php/login/flow";
    public static final String PROTOCOL_SUFFIX = "://";
    public static final String LOGIN_URL_DATA_KEY_VALUE_SEPARATOR = ":";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HTTP_PROTOCOL = "http://";

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
    private AccountSetupBinding accountSetupBinding;
    private AccountSetupWebviewBinding accountSetupWebviewBinding;

    private String mServerStatusText = EMPTY_STRING;
    private int mServerStatusIcon;

    private GetServerInfoOperation.ServerInfo mServerInfo = new GetServerInfoOperation.ServerInfo();

    /// Authentication PRE-Fragment elements
    private WebViewFidoBridge webViewFidoU2fBridge;
    private WebViewWebauthnBridge webViewWebauthnBridge;

    private String mAuthStatusText = EMPTY_STRING;
    private int mAuthStatusIcon;

    private AuthenticatorAsyncTask mAsyncTask;

    private boolean mIsFirstAuthAttempt;

    /// Identifier of operation in progress which result shouldn't be lost
    private long mWaitingForOpId = Long.MAX_VALUE;

    private boolean showWebViewLoginUrl;
    private String webViewUser;
    private String webViewPassword;

    @Inject UserAccountManager accountManager;
    @Inject AppPreferences preferences;
    @Inject OnboardingService onboarding;
    @Inject DeviceInfo deviceInfo;
    private boolean onlyAdd = false;
    @SuppressLint("ResourceAsColor") @ColorInt
    private int primaryColor = R.color.primary;
    private boolean strictMode = false;

    @VisibleForTesting
    public AccountSetupBinding getAccountSetupBinding() {
        return accountSetupBinding;
    }

    /**
     * {@inheritDoc}
     * <p>
     * IMPORTANT ENTRY POINT 1: activity is shown to the user
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        boolean directLogin = data != null && data.toString().startsWith(getString(R.string.login_data_own_scheme));
        if (savedInstanceState == null && !directLogin) {
            onboarding.launchFirstRunIfNeeded(this);
        }

        onlyAdd = getIntent().getBooleanExtra(KEY_ONLY_ADD, false) || checkIfViaSSO(getIntent());

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
        boolean webViewLoginMethod;
        if (getIntent().getBooleanExtra(EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)) {
            webViewLoginMethod = true;
            webloginUrl = getString(R.string.provider_registration_server);
        } else {
            webViewLoginMethod = !TextUtils.isEmpty(getResources().getString(R.string.webview_login_url));
            showWebViewLoginUrl = getResources().getBoolean(R.bool.show_server_url_input);
        }

        /// load user interface
        if (webViewLoginMethod) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            accountSetupWebviewBinding = AccountSetupWebviewBinding.inflate(getLayoutInflater());
            setContentView(accountSetupWebviewBinding.getRoot());
            initWebViewLogin(webloginUrl, false);
        } else {
            accountSetupBinding = AccountSetupBinding.inflate(getLayoutInflater());
            setContentView(accountSetupBinding.getRoot());

            /// initialize general UI elements
            initOverallUi();

            /// initialize block to be moved to single Fragment to check server and get info about it

            /// initialize block to be moved to single Fragment to retrieve and validate credentials
            initAuthorizationPreFragment(savedInstanceState);
        }

        initServerPreFragment(savedInstanceState);
    }

    private void deleteCookies() {
        try {
            CookieSyncManager.createInstance(this);
            CookieManager.getInstance().removeAllCookies(null);
        } catch (AndroidRuntimeException e) {
            Log_OC.e(TAG, e.getMessage());
        }
    }

    private static String getWebLoginUserAgent() {
        return Build.MANUFACTURER.substring(0, 1).toUpperCase(Locale.getDefault()) +
            Build.MANUFACTURER.substring(1).toLowerCase(Locale.getDefault()) + " " + Build.MODEL + " (Android)";
    }

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewLogin(String baseURL, boolean useGenericUserAgent) {
        accountSetupWebviewBinding.loginWebview.setVisibility(View.GONE);

        accountSetupWebviewBinding.loginWebview.getSettings().setAllowFileAccess(false);
        accountSetupWebviewBinding.loginWebview.getSettings().setJavaScriptEnabled(true);
        accountSetupWebviewBinding.loginWebview.getSettings().setDomStorageEnabled(true);

        if (useGenericUserAgent) {
            accountSetupWebviewBinding.loginWebview.getSettings().setUserAgentString(MainApp.getUserAgent());
        } else {
            accountSetupWebviewBinding.loginWebview.getSettings().setUserAgentString(getWebLoginUserAgent());
        }
        accountSetupWebviewBinding.loginWebview.getSettings().setSaveFormData(false);
        accountSetupWebviewBinding.loginWebview.getSettings().setSavePassword(false);

        FidoDialogOptions.Builder dialogOptionsBuilder = FidoDialogOptions.builder();
        dialogOptionsBuilder.setShowSdkLogo(true);
        dialogOptionsBuilder.setTheme(R.style.FidoDialog);
        webViewFidoU2fBridge = WebViewFidoBridge.createInstanceForWebView(
            this, accountSetupWebviewBinding.loginWebview, dialogOptionsBuilder);

        WebauthnDialogOptions.Builder webauthnOptionsBuilder = WebauthnDialogOptions.builder();
        webauthnOptionsBuilder.setShowSdkLogo(true);
        webauthnOptionsBuilder.setAllowSkipPin(true);
        webauthnOptionsBuilder.setTheme(R.style.FidoDialog);
        webViewWebauthnBridge = WebViewWebauthnBridge.createInstanceForWebView(
            this, accountSetupWebviewBinding.loginWebview, webauthnOptionsBuilder);

        Map<String, String> headers = new HashMap<>();
        headers.put(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

        String url;
        if (baseURL != null && !baseURL.isEmpty()) {
            url = baseURL;
        } else {
            url = getResources().getString(R.string.webview_login_url);
        }

        if (url.startsWith(HTTPS_PROTOCOL)) {
            strictMode = true;
        }

        accountSetupWebviewBinding.loginWebview.loadUrl(url, headers);

        setClient();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (accountSetupWebviewBinding != null && event.getAction() == KeyEvent.ACTION_DOWN &&
            keyCode == KeyEvent.KEYCODE_BACK) {
            if (accountSetupWebviewBinding.loginWebview.canGoBack()) {
                accountSetupWebviewBinding.loginWebview.goBack();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setClient() {
        accountSetupWebviewBinding.loginWebview.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                webViewFidoU2fBridge.delegateShouldInterceptRequest(view, request);
                webViewWebauthnBridge.delegateShouldInterceptRequest(view, request);
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                webViewFidoU2fBridge.delegateOnPageStarted(view, url, favicon);
                webViewWebauthnBridge.delegateOnPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/")) {
                    parseAndLoginFromWebView(url);
                    return true;
                }
                if (strictMode && url.startsWith(HTTP_PROTOCOL)) {
                    Snackbar.make(view, R.string.strict_mode, Snackbar.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                accountSetupWebviewBinding.loginWebviewProgressBar.setVisibility(View.GONE);
                accountSetupWebviewBinding.loginWebview.setVisibility(View.VISIBLE);

                ThemeToolbarUtils.colorStatusBar(AuthenticatorActivity.this, primaryColor);
                getWindow().setNavigationBarColor(primaryColor);
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
                accountSetupWebviewBinding.loginWebviewProgressBar.setVisibility(View.GONE);
                accountSetupWebviewBinding.loginWebview.setVisibility(View.VISIBLE);

                InputStream resources = getResources().openRawResource(R.raw.custom_error);
                String customError = DisplayUtils.getData(resources);

                if (!customError.isEmpty()) {
                    accountSetupWebviewBinding.loginWebview.loadData(customError, "text/html; charset=UTF-8", null);
                }
            }
        });
    }

    private void parseAndLoginFromWebView(String dataString) {
        try {
            String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
            LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, dataString);

            if (accountSetupBinding != null) {
                accountSetupBinding.hostUrlInput.setText("");
            }
            mServerInfo.mBaseUrl = AuthenticatorUrlUtils.normalizeUrlSuffix(loginUrlInfo.serverAddress);
            webViewUser = loginUrlInfo.username;
            webViewPassword = loginUrlInfo.password;
        } catch (Exception e) {
            mServerStatusIcon = R.drawable.ic_alert;
            mServerStatusText = getString(R.string.qr_could_not_be_read);
            showServerStatus();
        }
        checkOcServer();
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
        accountSetupBinding.hostUrlContainer.setEndIconOnClickListener(v -> checkOcServer());

        accountSetupBinding.hostUrlInputHelperText.setText(
            String.format(getString(R.string.login_url_helper_text), getString(R.string.app_name)));

        if (deviceInfo.hasCamera(this)) {
            accountSetupBinding.scanQr.setOnClickListener(v -> onScan());
            ThemeDrawableUtils.tintDrawable(accountSetupBinding.scanQr.getDrawable(),
                                            getResources().getColor(R.color.login_text_color));
        } else {
            accountSetupBinding.scanQr.setVisibility(View.GONE);
        }
    }

    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initServerPreFragment(Bundle savedInstanceState) {
        // step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        if (savedInstanceState == null) {
            if (mAccount != null) {
                String baseUrl = mAccountMgr.getUserData(mAccount, Constants.KEY_OC_BASE_URL);
                if (TextUtils.isEmpty(baseUrl)) {
                    mServerInfo.mBaseUrl = "";
                } else {
                    mServerInfo.mBaseUrl = baseUrl;
                }
                // TODO do next in a setter for mBaseUrl
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL);
                mServerInfo.mVersion = accountManager.getServerVersion(mAccount);
            } else {
                mServerInfo.mBaseUrl = getString(R.string.webview_login_url).trim();
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL);
            }
        } else {
            mServerStatusText = savedInstanceState.getString(KEY_SERVER_STATUS_TEXT);
            mServerStatusIcon = savedInstanceState.getInt(KEY_SERVER_STATUS_ICON);

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
    }

    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initAuthorizationPreFragment(Bundle savedInstanceState) {
        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        if (savedInstanceState != null) {
            mAuthStatusText = savedInstanceState.getString(KEY_AUTH_STATUS_TEXT);
            mAuthStatusIcon = savedInstanceState.getInt(KEY_AUTH_STATUS_ICON);
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        showAuthStatus();

        accountSetupBinding.hostUrlInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        accountSetupBinding.hostUrlInput.setOnEditorActionListener(this);
    }

    /**
     * Saves relevant state before {@link #onPause()}
     * <p>
     * See {@link super#onSaveInstanceState(Bundle)}
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //Log_OC.e(TAG, "onSaveInstanceState init" );
        super.onSaveInstanceState(outState);

        /// global state
        outState.putLong(KEY_WAITING_FOR_OP_ID, mWaitingForOpId);

        outState.putBoolean(KEY_IS_SSL_CONN, mServerInfo.mIsSslConn);
        outState.putString(KEY_HOST_URL_TEXT, mServerInfo.mBaseUrl);
        if (mServerInfo.mVersion != null) {
            outState.putString(KEY_OC_VERSION, mServerInfo.mVersion.getVersion());
        }
        outState.putString(KEY_SERVER_AUTH_METHOD, mServerInfo.mAuthMethod.name());

        /// authentication
        outState.putBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG, mIsFirstAuthAttempt);

        /// AsyncTask (User and password)
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, true);
        } else {
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, false);
        }
        mAsyncTask = null;
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

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
     * The redirection triggered by the OAuth authentication server as response to the GET AUTHORIZATION request is
     * caught here.
     * <p>
     * To make this possible, this activity needs to be qualified with android:launchMode = "singleTask" in the
     * AndroidManifest.xml file.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log_OC.d(TAG, "onNewIntent()");

        if (intent.getBooleanExtra(FirstRunActivity.EXTRA_EXIT, false)) {
            super.finish();
        }

        onlyAdd = intent.getBooleanExtra(KEY_ONLY_ADD, false) || checkIfViaSSO(intent);

        // Passcode
        PassCodeManager passCodeManager = new PassCodeManager(preferences);
        passCodeManager.onActivityStarted(this);

        Uri data = intent.getData();

        if (data != null && data.toString().startsWith(getString(R.string.login_data_own_scheme))) {
            if (!getResources().getBoolean(R.bool.multiaccount_support) &&
                accountManager.getAccounts().length == 1) {
                Toast.makeText(this, R.string.no_mutliple_accounts_allowed, Toast.LENGTH_LONG).show();
                finish();
                return;
            } else {
                parseAndLoginFromWebView(data.toString());
            }
        }

        if (intent.getBooleanExtra(EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)) {
            accountSetupWebviewBinding = AccountSetupWebviewBinding.inflate(getLayoutInflater());
            setContentView(accountSetupWebviewBinding.getRoot());
            initWebViewLogin(getString(R.string.provider_registration_server), true);
        }
    }

    private boolean checkIfViaSSO(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return false;
        } else {
            String authTokenType = extras.getString("authTokenType");
            return "SSO".equals(authTokenType);
        }
    }


    /**
     * The redirection triggered by the OAuth authentication server as response to the GET AUTHORIZATION, and deferred
     * in {@link #onNewIntent(Intent)}, is processed here.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        if (!bindService(new Intent(this, OperationsService.class),
                         mOperationsServiceConnection,
                         Context.BIND_AUTO_CREATE)) {
            DisplayUtils.showSnackMessage(accountSetupBinding.scroll, R.string.error_cant_bind_to_operations_service);
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

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        super.onDestroy();
    }


    private void checkOcServer() {
        String uri;
        if (accountSetupBinding != null && accountSetupBinding.hostUrlInput.getText()!= null &&
            !accountSetupBinding.hostUrlInput.getText().toString().isEmpty()) {
            uri = accountSetupBinding.hostUrlInput.getText().toString().trim();
        } else {
            uri = mServerInfo.mBaseUrl;
        }

        mServerInfo = new GetServerInfoOperation.ServerInfo();

        if (uri.length() != 0) {
            if (accountSetupBinding != null) {
                uri = AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles(uri);
                accountSetupBinding.hostUrlInput.setText(uri);
            }

            uri = AuthenticatorUrlUtils.normalizeScheme(uri);

            // Handle internationalized domain names
            try {
                uri = DisplayUtils.convertIdn(uri, true);
            } catch (IllegalArgumentException ex) {
                // Let Owncloud library check the error of the malformed URI
                Log_OC.e(TAG, "Error converting internationalized domain name " + uri, ex);
            }

            if (accountSetupBinding != null) {
                mServerStatusText = getResources().getString(R.string.auth_testing_connection);
                mServerStatusIcon = R.drawable.progress_small;
                showServerStatus();
            }

            // TODO maybe do this via async task
            Intent getServerInfoIntent = new Intent();
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
            getServerInfoIntent.putExtra(OperationsService.EXTRA_SERVER_URL,
                                         AuthenticatorUrlUtils.normalizeUrlSuffix(uri));

            if (mOperationsServiceBinder != null) {
                mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getServerInfoIntent);
            } else {
                Log_OC.e(TAG, "Server check tried with OperationService unbound!");
            }

        }
    }

    /**
     * Tests the credentials entered by the user performing a check of existence on the root folder of the ownCloud
     * server.
     */
    private void checkBasicAuthorization(@Nullable String webViewUsername, @Nullable String webViewPassword) {
        // be gentle with the user
        IndeterminateProgressDialog dialog = IndeterminateProgressDialog.newInstance(R.string.auth_trying_to_login,
                                                                                     true);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, WAIT_DIALOG_TAG);
        ft.commitAllowingStateLoss();

        // validate credentials accessing the root folder
        OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBasicCredentials(webViewUsername,
                                                                                         webViewPassword);
        accessRootFolder(credentials);
    }

    private void accessRootFolder(OwnCloudCredentials credentials) {
        mAsyncTask = new AuthenticatorAsyncTask(this);
        Object[] params = {mServerInfo.mBaseUrl, credentials};
        mAsyncTask.execute(params);
    }

    /**
     * Callback method invoked when a RemoteOperation executed by this Activity finishes.
     * <p>
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

    private void onGetUserNameFinish(RemoteOperationResult<UserInfo> result) {
        mWaitingForOpId = Long.MAX_VALUE;
        if (result.isSuccess()) {
            boolean success = false;

            if (mAction == ACTION_CREATE) {
                success = createAccount(result);
            } else {
                try {
                    updateAccountAuthentication();
                    success = true;

                } catch (AccountNotFoundException e) {
                    Log_OC.e(TAG, "Account " + mAccount + " was removed!", e);
                    DisplayUtils.showSnackMessage(accountSetupBinding.scroll, R.string.auth_account_does_not_exist);
                    finish();
                }
            }

            if (success) {
                finish();
            }
        } else {
            // TODO check
            int statusText = result.getCode() == ResultCode.MAINTENANCE_MODE ? R.string.maintenance_mode : R.string.auth_fail_get_user_name;
            updateStatusIconFailUserName(statusText);
            showAuthStatus();
            Log_OC.e(TAG, "Access to user name failed: " + result.getLogMessage());
        }
    }

    /**
     * Processes the result of the server check performed when the user finishes the enter of the server URL.
     *
     * @param result Result of the check.
     */
    private void onGetServerInfoFinish(RemoteOperationResult result) {
        /// update activity state
        mWaitingForOpId = Long.MAX_VALUE;

        if (result.isSuccess()) {
            /// SUCCESS means:
            //      1. connection succeeded, and we know if it's SSL or not
            //      2. server is installed
            //      3. we got the server version
            //      4. we got the authentication method required by the server
            mServerInfo = (GetServerInfoOperation.ServerInfo) (result.getData().get(0));

            // show outdated warning
            if (getResources().getBoolean(R.bool.show_outdated_server_warning) &&
                MainApp.OUTDATED_SERVER_VERSION.isSameMajorVersion(mServerInfo.mVersion) &&
                !mServerInfo.hasExtendedSupport) {
                DisplayUtils.showServerOutdatedSnackbar(this, Snackbar.LENGTH_INDEFINITE);
            }

            if (webViewUser != null && !webViewUser.isEmpty() &&
                webViewPassword != null && !webViewPassword.isEmpty()) {
                checkBasicAuthorization(webViewUser, webViewPassword);
            } else {
                new Thread(() -> {
                    OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(mServerInfo.mBaseUrl),
                                                                                       this,
                                                                                       true);
                    RemoteOperationResult remoteOperationResult = new GetCapabilitiesRemoteOperation().execute(client);

                    if (remoteOperationResult.isSuccess() &&
                        remoteOperationResult.getData() != null &&
                        remoteOperationResult.getData().size() > 0) {
                        OCCapability capability = (OCCapability) remoteOperationResult.getData().get(0);
                        try {
                            primaryColor = Color.parseColor(capability.getServerColor());
                        } catch (Exception e) {
                            // falls back to primary color
                        }
                    }
                }).start();

                accountSetupWebviewBinding = AccountSetupWebviewBinding.inflate(getLayoutInflater());
                setContentView(accountSetupWebviewBinding.getRoot());
                initWebViewLogin(mServerInfo.mBaseUrl + WEB_LOGIN, false);
            }
        } else {
            updateServerStatusIconAndText(result);
            showServerStatus();
        }

        // very special case (TODO: move to a common place for all the remote operations)
        if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result);
        }
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
                if (accountSetupBinding.hostUrlInput.getText() != null &&
                    accountSetupBinding.hostUrlInput
                    .getText()
                    .toString()
                    .trim()
                    .toLowerCase(Locale.ROOT)
                    .startsWith(HTTP_PROTOCOL)) {
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
                if (showWebViewLoginUrl) {
                    if (accountSetupBinding.hostUrlInput.getText() != null &&
                        accountSetupBinding.hostUrlInput
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        .startsWith(HTTP_PROTOCOL)) {
                        mAuthStatusText = getResources().getString(R.string.auth_connection_established);
                        mAuthStatusIcon = R.drawable.ic_ok;
                    } else {
                        mAuthStatusText = getResources().getString(R.string.auth_nossl_plain_ok_title);
                        mAuthStatusIcon = R.drawable.ic_lock_open_white;
                    }
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
            case ACCOUNT_NOT_NEW:
                mAuthStatusText = getString(R.string.auth_account_not_new);
                if (!showWebViewLoginUrl) {
                    DisplayUtils.showErrorAndFinishActivity(this, mAuthStatusText);
                }
                break;
            case UNHANDLED_HTTP_CODE:
            default:
                mAuthStatusText = ErrorMessageAdapter.getErrorCauseMessage(result, null, getResources());
                if (!showWebViewLoginUrl) {
                    DisplayUtils.showErrorAndFinishActivity(this, mAuthStatusText);
                }
        }
    }

    private void updateStatusIconFailUserName(int failedStatusText) {
        mAuthStatusIcon = R.drawable.ic_alert;
        mAuthStatusText = getResources().getString(failedStatusText);
    }

    /**
     * Processes the result of the access check performed to try the user credentials.
     * <p>
     * Creates a new account through the AccountManager.
     *
     * @param result Result of the operation.
     */
    @Override
    public void onAuthenticatorTaskCallback(RemoteOperationResult<UserInfo> result) {
        mWaitingForOpId = Long.MAX_VALUE;
        dismissWaitingDialog();
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
                    DisplayUtils.showSnackMessage(accountSetupBinding.scroll, R.string.auth_account_does_not_exist);
                    finish();
                }
            }

            // Reset webView
            webViewPassword = null;
            webViewUser = null;
            deleteCookies();

            if (success) {
                accountManager.setCurrentOwnCloudAccount(mAccount.name);
                if (onlyAdd) {
                    finish();
                } else {
                    Intent i = new Intent(this, FileDisplayActivity.class);
                    i.setAction(FileDisplayActivity.RESTART);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            } else {
                // init webView again
                if (accountSetupWebviewBinding != null) {
                    accountSetupWebviewBinding.loginWebview.setVisibility(View.GONE);
                }
                accountSetupBinding = AccountSetupBinding.inflate(getLayoutInflater());
                setContentView(accountSetupBinding.getRoot());
                initOverallUi();

                accountSetupBinding.hostUrlInput.setText(mServerInfo.mBaseUrl);
                accountSetupBinding.serverStatusText.setVisibility(View.GONE);
                showAuthStatus();
            }

        } else if (result.isServerFail() || result.isException()) {
            /// server errors or exceptions in authorization take to requiring a new check of the server
            mServerInfo = new GetServerInfoOperation.ServerInfo();

            // update status icon and text
            updateServerStatusIconAndText(result);
            showServerStatus();
            mAuthStatusIcon = 0;
            mAuthStatusText = EMPTY_STRING;

            // very special case (TODO: move to a common place for all the remote operations)
            if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
                showUntrustedCertDialog(result);
            }

        } else {    // authorization fail due to client side - probably wrong credentials
            if (accountSetupWebviewBinding != null) {
                initWebViewLogin(mServerInfo.mBaseUrl + WEB_LOGIN, false);
                DisplayUtils.showSnackMessage(this,
                                              accountSetupWebviewBinding.loginWebview, R.string.auth_access_failed,
                                              result.getLogMessage());
            } else {
                DisplayUtils.showSnackMessage(this, R.string.auth_access_failed, result.getLogMessage());

                // init webView again
                updateAuthStatusIconAndText(result);
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
     * <p>
     * Sets the proper response so that the AccountAuthenticator that started this activity saves a new authorization
     * token for mAccount.
     * <p>
     * Kills the session kept by OwnCloudClientManager so that a new one will created with the new credentials when
     * needed.
     */
    private void updateAccountAuthentication() throws AccountNotFoundException {
        Bundle response = new Bundle();
        response.putString(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
        response.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccount.type);
        response.putString(AccountManager.KEY_AUTHTOKEN, webViewPassword);
        mAccountMgr.setPassword(mAccount, webViewPassword);

        // remove managed clients for this account to enforce creation with fresh credentials
        OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, this);
        OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount);

        setAccountAuthenticatorResult(response);
        Intent intent = new Intent();
        intent.putExtras(response);
        setResult(RESULT_OK, intent);
    }

    /**
     * Creates a new account through the Account Authenticator that started this activity.
     * <p>
     * This makes the account permanent.
     * <p>
     * TODO Decide how to name the OAuth accounts
     */
    @SuppressFBWarnings("DMI")
    @SuppressLint("TrulyRandom")
    protected boolean createAccount(RemoteOperationResult<UserInfo> authResult) {
        String accountType = MainApp.getAccountType(this);

        // create and save new ownCloud account
        String lastPermanentLocation = authResult.getLastPermanentLocation();
        if (lastPermanentLocation != null) {
            mServerInfo.mBaseUrl = AuthenticatorUrlUtils.trimWebdavSuffix(lastPermanentLocation);
        }

        Uri uri = Uri.parse(mServerInfo.mBaseUrl);
        // used for authenticate on every login/network connection, determined by first login (weblogin/old login)
        // can be anything: email, name, name with whitespaces
        String loginName = webViewUser;

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
            UserInfo userInfo = authResult.getResultData();
            if (userInfo == null) {
                Log_OC.e(this, "Could not read user data!");
                return false;
            }

            mAccount = newAccount;
            mAccountMgr.addAccountExplicitly(mAccount, webViewPassword, null);

            // add the new account as default in preferences, if there is none already
            User defaultAccount = accountManager.getUser();
            if (defaultAccount.isAnonymous()) {
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
            mAccountMgr.setUserData(mAccount, Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
            mAccountMgr.setUserData(mAccount, Constants.KEY_USER_ID, userInfo.getId());
            mAccountMgr.setUserData(mAccount,
                                    Constants.KEY_OC_ACCOUNT_VERSION,
                                    Integer.toString(UserAccountManager.ACCOUNT_VERSION_WITH_PROPER_ID));


            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);

            // notify Document Provider
            DocumentsStorageProvider.notifyRootsChanged(this);

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
        Intent intent = new Intent(this, QrCodeActivity.class);
        startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSIONS_CAMERA) {// If request is cancelled, result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                startQRScanner();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * /** Updates the content and visibility state of the icon and text associated to the last check on the ownCloud
     * server.
     */
    private void showServerStatus() {
        if (mServerStatusIcon == NO_ICON && EMPTY_STRING.equals(mServerStatusText)) {
            accountSetupBinding.serverStatusText.setVisibility(View.INVISIBLE);
        } else {
            accountSetupBinding.serverStatusText.setText(mServerStatusText);
            accountSetupBinding.serverStatusText.setCompoundDrawablesWithIntrinsicBounds(mServerStatusIcon, 0, 0, 0);
            accountSetupBinding.serverStatusText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the content and visibility state of the icon and text associated to the interactions with the OAuth
     * authorization server.
     */
    private void showAuthStatus() {
        if (accountSetupBinding != null) {
            if (mAuthStatusIcon == NO_ICON && EMPTY_STRING.equals(mAuthStatusText)) {
                accountSetupBinding.authStatusText.setVisibility(View.INVISIBLE);
            } else {
                accountSetupBinding.authStatusText.setText(mAuthStatusText);
                accountSetupBinding.authStatusText.setCompoundDrawablesWithIntrinsicBounds(mAuthStatusIcon, 0, 0, 0);
                accountSetupBinding.authStatusText.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Called when the 'action' button in an IME is pressed ('enter' in software keyboard).
     * <p>
     * Used to trigger the authentication check when the user presses 'enter' after writing the password, or to throw
     * the server test when the only field on screen is the URL input field.
     */
    @Override
    public boolean onEditorAction(TextView inputField, int actionId, KeyEvent event) {
        if ((actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL)
            && inputField != null && inputField.equals(accountSetupBinding.hostUrlInput)) {
            checkOcServer();
        }
        return false;   // always return false to grant that the software keyboard is hidden anyway
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
        mOperationsServiceBinder.addOperationListener(this, mHandler);
        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int) mWaitingForOpId, this);
        }
    }


    private void dismissWaitingDialog() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(WAIT_DIALOG_TAG);
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
                    try {
                        String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
                        LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, data.toString());

                        mServerInfo.mBaseUrl = AuthenticatorUrlUtils.normalizeUrlSuffix(loginUrlInfo.serverAddress);
                        webViewUser = loginUrlInfo.username;
                        webViewPassword = loginUrlInfo.password;
                        doOnResumeAndBound();
                        checkOcServer();
                    } catch (Exception e) {
                        mServerStatusIcon = R.drawable.ic_alert;
                        mServerStatusText = getString(R.string.qr_could_not_be_read);
                        showServerStatus();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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

            if (!getResources().getBoolean(R.bool.multiaccount_support) &&
                accountManager.getAccounts().length == 1) {
                Toast.makeText(this, R.string.no_mutliple_accounts_allowed, Toast.LENGTH_LONG).show();
            } else {
                parseAndLoginFromWebView(result);
            }
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
