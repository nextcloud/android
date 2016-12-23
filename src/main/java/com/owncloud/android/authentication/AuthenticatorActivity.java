/**
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.SsoWebViewClient.SsoWebViewClientListener;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.operations.DetectAuthenticationMethodOperation.AuthenticationMethod;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.operations.OAuth2GetAccessToken;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.components.CustomEditText;
import com.owncloud.android.ui.dialog.CredentialsDialogFragment;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.ui.dialog.SamlWebViewDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;

import java.security.cert.X509Certificate;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This Activity is used to add an ownCloud account to the App
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements OnRemoteOperationListener, OnFocusChangeListener, OnEditorActionListener,
        SsoWebViewClientListener, OnSslUntrustedCertListener,
        AuthenticatorAsyncTask.OnAuthenticatorTaskListener {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    private static final String SCREEN_NAME = "Login";

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private static final String KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";

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

    private static final String AUTH_ON = "on";
    private static final String AUTH_OPTIONAL = "optional";

    public static final byte ACTION_CREATE = 0;
    public static final byte ACTION_UPDATE_TOKEN = 1;               // requested by the user
    public static final byte ACTION_UPDATE_EXPIRED_TOKEN = 2;       // detected by the app

    private static final String UNTRUSTED_CERT_DIALOG_TAG = "UNTRUSTED_CERT_DIALOG";
    private static final String SAML_DIALOG_TAG = "SAML_DIALOG";
    private static final String WAIT_DIALOG_TAG = "WAIT_DIALOG";
    private static final String CREDENTIALS_DIALOG_TAG = "CREDENTIALS_DIALOG";
    private static final String KEY_AUTH_IS_FIRST_ATTEMPT_TAG = "KEY_AUTH_IS_FIRST_ATTEMPT";

    private static final String KEY_USERNAME = "USERNAME";
    private static final String KEY_PASSWORD = "PASSWORD";
    private static final String KEY_ASYNC_TASK_IN_PROGRESS = "AUTH_IN_PROGRESS";
    public static final String PROTOCOL_SUFFIX = "://";
    public static final String LOGIN_URL_DATA_KEY_VALUE_SEPARATOR = ":";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HTTP_PROTOCOL = "http://";

    public static final String REGULAR_SERVER_INPUT_TYPE = "regular";
    public static final String SUBDOMAIN_SERVER_INPUT_TYPE = "prefix";
    public static final String DIRECTORY_SERVER_INPUT_TYPE = "suffix";

    /// parameters from EXTRAs in starter Intent
    private byte mAction;
    private Account mAccount;
    private String mAuthTokenType;


    /// activity-level references / state
    private final Handler mHandler = new Handler();
    private ServiceConnection mOperationsServiceConnection = null;
    private OperationsServiceBinder mOperationsServiceBinder = null;
    private AccountManager mAccountMgr;
    private Uri mNewCapturedUriFromOAuth2Redirection;


    /// Server PRE-Fragment elements 
    private CustomEditText mHostUrlInput;
    private View mRefreshButton;
    private TextView mServerStatusView;

    private TextWatcher mHostUrlInputWatcher;
    private int mServerStatusText = 0, mServerStatusIcon = 0;

    private boolean mServerIsChecked = false;
    private boolean mServerIsValid = false;

    private GetServerInfoOperation.ServerInfo mServerInfo = new GetServerInfoOperation.ServerInfo();


    /// Authentication PRE-Fragment elements 
    private CheckBox mOAuth2Check;
    private TextView mOAuthAuthEndpointText;
    private TextView mOAuthTokenEndpointText;
    private EditText mUsernameInput;
    private EditText mPasswordInput;
    private View mOkButton;
    private TextView mAuthStatusView;

    private WebView mLoginWebView;

    private int mAuthStatusText = 0, mAuthStatusIcon = 0;

    private String mAuthToken = "";
    private AuthenticatorAsyncTask mAsyncTask;

    private boolean mIsFirstAuthAttempt;

    /// Identifier of operation in progress which result shouldn't be lost 
    private long mWaitingForOpId = Long.MAX_VALUE;

    private final String BASIC_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypePass(MainApp.getAccountType());
    private final String OAUTH_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType());
    private final String SAML_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType());

    private boolean webViewLoginMethod;
    private String webViewUser;
    private String webViewPassword;

    /**
     * {@inheritDoc}
     *
     * IMPORTANT ENTRY POINT 1: activity is shown to the user
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log_OC.e(TAG,  "onCreate init");
        super.onCreate(savedInstanceState);

        // Workaround, for fixing a problem with Android Library Suppor v7 19
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
        mNewCapturedUriFromOAuth2Redirection = null;

        /// get input values
        mAction = getIntent().getByteExtra(EXTRA_ACTION, ACTION_CREATE);
        mAccount = getIntent().getExtras().getParcelable(EXTRA_ACCOUNT);
        if (savedInstanceState == null) {
            initAuthTokenType();
        } else {
            mAuthTokenType = savedInstanceState.getString(KEY_AUTH_TOKEN_TYPE);
            mWaitingForOpId = savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID);
            mIsFirstAuthAttempt = savedInstanceState.getBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG);
        }

        webViewLoginMethod = !TextUtils.isEmpty(getResources().getString(R.string.webview_login_url));

        if (webViewLoginMethod) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        /// load user interface
        if (!webViewLoginMethod) {
            setContentView(R.layout.account_setup);

            /// initialize general UI elements
            initOverallUi();

            mOkButton = findViewById(R.id.buttonOK);
            mOkButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onOkClick();
                }
            });

            findViewById(R.id.centeredRefreshButton).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    checkOcServer();
                }
            });

            findViewById(R.id.embeddedRefreshButton).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    checkOcServer();
                }
            });

            /// initialize block to be moved to single Fragment to check server and get info about it

            /// initialize block to be moved to single Fragment to retrieve and validate credentials
            initAuthorizationPreFragment(savedInstanceState);

        } else {
            setContentView(R.layout.account_setup_webview);
            mLoginWebView = (WebView) findViewById(R.id.login_webview);
            initWebViewLogin();
        }

        initServerPreFragment(savedInstanceState);
    }

    private void initWebViewLogin() {
        mLoginWebView.getSettings().setAllowFileAccess(false);
        mLoginWebView.getSettings().setJavaScriptEnabled(true);
        mLoginWebView.getSettings().setUserAgentString(MainApp.getUserAgent());
        mLoginWebView.loadUrl(getResources().getString(R.string.webview_login_url));
        mLoginWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/")) {
                    parseAndLoginFromWebView(url);
                    return true;
                }
                return false;
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                mLoginWebView.loadData(DisplayUtils.getData(getResources().openRawResource(R.raw.custom_error)),"text/html; charset=UTF-8", null);
            }
        });
    }

    private void parseAndLoginFromWebView(String dataString) {
        String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
        LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, dataString);

        if (loginUrlInfo != null) {
            mServerInfo.mBaseUrl = normalizeUrlSuffix(loginUrlInfo.serverAddress);
            webViewUser = loginUrlInfo.username;
            webViewPassword = loginUrlInfo.password;
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
        LoginUrlInfo loginUrlInfo = new LoginUrlInfo();

        // format is basically xxx://login/server:xxx&user:xxx&password while all variables are optional
        String data = dataString.substring(prefix.length());

        // parse data
        String[] values = data.split("&");

        if (values.length < 1 || values.length > 3) {
            // error illegal number of URL elements detected
            throw new IllegalArgumentException("Illegal number of login URL elements detected: " + values.length);
        }

        for (String value : values) {
            if (value.startsWith("user" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR)) {
                loginUrlInfo.username = value.substring(("user" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR).length());
            } else if (value.startsWith("password" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR)) {
                loginUrlInfo.password = value.substring(("password" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR).length());
            } else if (value.startsWith("server" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR)) {
                loginUrlInfo.serverAddress = value.substring(("server" + LOGIN_URL_DATA_KEY_VALUE_SEPARATOR).length());
            } else {
                // error illegal URL element detected
                throw new IllegalArgumentException("Illegal magic login URL element detected: " + value);
            }
        }

        return loginUrlInfo;
    }

    private void initAuthTokenType() {
        mAuthTokenType = getIntent().getExtras().getString(AccountAuthenticator.KEY_AUTH_TOKEN_TYPE);
        if (mAuthTokenType == null) {
            if (mAccount != null) {
                boolean oAuthRequired = (mAccountMgr.getUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2) != null);
                boolean samlWebSsoRequired = (
                        mAccountMgr.getUserData
                                (mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null
                );
                mAuthTokenType = chooseAuthTokenType(oAuthRequired, samlWebSsoRequired);

            } else {
                boolean oAuthSupported = AUTH_ON.equals(getString(R.string.auth_method_oauth2));
                boolean samlWebSsoSupported = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));
                mAuthTokenType = chooseAuthTokenType(oAuthSupported, samlWebSsoSupported);
            }
        }
    }

    private String chooseAuthTokenType(boolean oauth, boolean saml) {
        if (saml) {
            return SAML_TOKEN_TYPE;
        } else if (oauth) {
            return OAUTH_TOKEN_TYPE;
        } else {
            return BASIC_TOKEN_TYPE;
        }
    }


    /**
     * Configures elements in the user interface under direct control of the Activity.
     */
    private void initOverallUi() {

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        boolean isWelcomeLinkVisible = getResources().getBoolean(R.bool.show_welcome_link);

        String instructionsMessageText = null;
        if (mAction == ACTION_UPDATE_EXPIRED_TOKEN) {
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType()).equals(mAuthTokenType)) {
                instructionsMessageText = getString(R.string.auth_expired_oauth_token_toast);

            } else if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType())
                    .equals(mAuthTokenType)) {
                instructionsMessageText = getString(R.string.auth_expired_saml_sso_token_toast);

            } else {
                instructionsMessageText = getString(R.string.auth_expired_basic_auth_toast);
            }
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        Button welcomeLink = (Button) findViewById(R.id.welcome_link);
        welcomeLink.setVisibility(isWelcomeLinkVisible ? View.VISIBLE : View.GONE);
        welcomeLink.setText(String.format(getString(R.string.auth_register), getString(R.string.app_name)));

        TextView instructionsView = (TextView) findViewById(R.id.instructions_message);
        if (instructionsMessageText != null) {
            instructionsView.setVisibility(View.VISIBLE);
            instructionsView.setText(instructionsMessageText);
        } else {
            instructionsView.setVisibility(View.GONE);
        }
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
                mServerInfo.mVersion = AccountUtils.getServerVersion(mAccount);
            } else {
                if (!webViewLoginMethod) {
                    mServerInfo.mBaseUrl = getString(R.string.server_url).trim();
                } else {
                    mServerInfo.mBaseUrl = getString(R.string.webview_login_url).trim();
                }
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL);
            }
        } else {
            mServerStatusText = savedInstanceState.getInt(KEY_SERVER_STATUS_TEXT);
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
            mHostUrlInput = (CustomEditText) findViewById(R.id.hostUrlInput);
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
            mServerStatusView = (TextView) findViewById(R.id.server_status_text);
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
                                    normalizeUrl(s.toString(), mServerInfo.mIsSslConn))) {
                        mOkButton.setEnabled(false);
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mAuthStatusIcon != 0) {
                        Log_OC.d(TAG, "onTextChanged: hiding authentication status");
                        mAuthStatusIcon = 0;
                        mAuthStatusText = 0;
                        showAuthStatus();
                    }
                }
            };


            // TODO find out if this is really necessary, or if it can done in a different way
            findViewById(R.id.scroll).setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN &&
                            AccountTypeUtils
                                    .getAuthTokenTypeSamlSessionCookie(MainApp
                                            .getAccountType()).equals(mAuthTokenType) &&
                            mHostUrlInput.hasFocus()) {
                        checkOcServer();
                    }
                    return false;
                }
            });
        }
    }


    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initAuthorizationPreFragment(Bundle savedInstanceState) {

        /// step 0 - get UI elements in layout
        mOAuth2Check = (CheckBox) findViewById(R.id.oauth_onOff_check);
        mOAuthAuthEndpointText = (TextView) findViewById(R.id.oAuthEntryPoint_1);
        mOAuthTokenEndpointText = (TextView) findViewById(R.id.oAuthEntryPoint_2);
        mUsernameInput = (EditText) findViewById(R.id.account_username);
        mPasswordInput = (EditText) findViewById(R.id.account_password);
        mAuthStatusView = (TextView) findViewById(R.id.auth_status_text);

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        String presetUserName = null;
        boolean isPasswordExposed = false;
        if (savedInstanceState == null) {
            if (mAccount != null) {
                presetUserName = com.owncloud.android.lib.common.accounts.AccountUtils.getUsernameForAccount(mAccount);
            }
        } else {
            isPasswordExposed = savedInstanceState.getBoolean(KEY_PASSWORD_EXPOSED, false);
            mAuthStatusText = savedInstanceState.getInt(KEY_AUTH_STATUS_TEXT);
            mAuthStatusIcon = savedInstanceState.getInt(KEY_AUTH_STATUS_ICON);
            mAuthToken = savedInstanceState.getString(KEY_AUTH_TOKEN);
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        mOAuth2Check.setChecked(
                AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType())
                        .equals(mAuthTokenType));
        if (presetUserName != null) {
            mUsernameInput.setText(presetUserName);
        }
        if (mAction != ACTION_CREATE) {
            mUsernameInput.setEnabled(false);
            mUsernameInput.setFocusable(false);
        }
        mPasswordInput.setText(""); // clean password to avoid social hacking
        if (isPasswordExposed) {
            showPassword();
        }
        updateAuthenticationPreFragmentVisibility();
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
     * Changes the visibility of input elements depending on
     * the current authorization method.
     */
    private void updateAuthenticationPreFragmentVisibility() {
        if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType)) {
            // SAML-based web Single Sign On
            mOAuth2Check.setVisibility(View.GONE);
            mOAuthAuthEndpointText.setVisibility(View.GONE);
            mOAuthTokenEndpointText.setVisibility(View.GONE);
            mUsernameInput.setVisibility(View.GONE);
            mPasswordInput.setVisibility(View.GONE);

        } else {
            if (mAction == ACTION_CREATE &&
                    AUTH_OPTIONAL.equals(getString(R.string.auth_method_oauth2))) {
                mOAuth2Check.setVisibility(View.VISIBLE);
            } else {
                mOAuth2Check.setVisibility(View.GONE);
            }

            if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType()).
                    equals(mAuthTokenType)) {
                // OAuth 2 authorization

                mOAuthAuthEndpointText.setVisibility(View.VISIBLE);
                mOAuthTokenEndpointText.setVisibility(View.VISIBLE);
                mUsernameInput.setVisibility(View.GONE);
                mPasswordInput.setVisibility(View.GONE);

            } else {
                // basic HTTP authorization
                mOAuthAuthEndpointText.setVisibility(View.GONE);
                mOAuthTokenEndpointText.setVisibility(View.GONE);
                mUsernameInput.setVisibility(View.VISIBLE);
                mPasswordInput.setVisibility(View.VISIBLE);
            }
        }
    }


    /**
     * Saves relevant state before {@link #onPause()}
     *
     * Do NOT save {@link #mNewCapturedUriFromOAuth2Redirection}; it keeps a temporal flag,
     * intended to defer the processing of the redirection caught in
     * {@link #onNewIntent(Intent)} until {@link #onResume()}
     *
     * See {@link super#onSaveInstanceState(Bundle)}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Log_OC.e(TAG, "onSaveInstanceState init" );
        super.onSaveInstanceState(outState);

        /// global state
        outState.putString(KEY_AUTH_TOKEN_TYPE, mAuthTokenType);
        outState.putLong(KEY_WAITING_FOR_OP_ID, mWaitingForOpId);

        if (!webViewLoginMethod) {
            /// Server PRE-fragment state
            outState.putInt(KEY_SERVER_STATUS_TEXT, mServerStatusText);
            outState.putInt(KEY_SERVER_STATUS_ICON, mServerStatusIcon);
            outState.putBoolean(KEY_SERVER_CHECKED, mServerIsChecked);
            outState.putBoolean(KEY_SERVER_VALID, mServerIsValid);

            /// Authentication PRE-fragment state
            outState.putBoolean(KEY_PASSWORD_EXPOSED, isPasswordVisible());
            outState.putInt(KEY_AUTH_STATUS_ICON, mAuthStatusIcon);
            outState.putInt(KEY_AUTH_STATUS_TEXT, mAuthStatusText);
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

            OwnCloudCredentials credentials = null;
            if (BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
                credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password);

            } else if (OAUTH_TOKEN_TYPE.equals(mAuthTokenType)) {
                credentials = OwnCloudCredentialsFactory.newBearerCredentials(mAuthToken);

            }
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
        Uri data = intent.getData();
        if (data != null && data.toString().startsWith(getString(R.string.oauth2_redirect_uri))) {
            mNewCapturedUriFromOAuth2Redirection = data;
        }
    }


    /**
     * The redirection triggered by the OAuth authentication server as response to the
     * GET AUTHORIZATION, and deferred in {@link #onNewIntent(Intent)}, is processed here.
     */
    @Override
    protected void onResume() {
        super.onResume();

        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);

        if (!webViewLoginMethod) {
            // bound here to avoid spurious changes triggered by Android on device rotations
            mHostUrlInput.setOnFocusChangeListener(this);
            mHostUrlInput.addTextChangedListener(mHostUrlInputWatcher);

            if (mNewCapturedUriFromOAuth2Redirection != null) {
                getOAuth2AccessTokenFromCapturedRedirection();
            }

            String dataString = getIntent().getDataString();
            if (dataString != null) {
                try {
                    populateLoginFields(dataString);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, "Illegal login data URL used", Toast.LENGTH_SHORT).show();
                    Log_OC.e(TAG, "Illegal login data URL used, no Login pre-fill!", e);
                }
            }
        }

        // bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        if (!bindService(new Intent(this, OperationsService.class),
                mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            Toast.makeText(this,
                    R.string.error_cant_bind_to_operations_service,
                    Toast.LENGTH_LONG)
                    .show();
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
     * Parses the redirection with the response to the GET AUTHORIZATION request to the
     * oAuth server and requests for the access token (GET ACCESS TOKEN)
     */
    private void getOAuth2AccessTokenFromCapturedRedirection() {
        /// Parse data from OAuth redirection
        String queryParameters = mNewCapturedUriFromOAuth2Redirection.getQuery();
        mNewCapturedUriFromOAuth2Redirection = null;

        /// Showing the dialog with instructions for the user.
        IndeterminateProgressDialog dialog =
                IndeterminateProgressDialog.newInstance(R.string.auth_getting_authorization, true);
        dialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);

        /// GET ACCESS TOKEN to the oAuth server
        Intent getServerInfoIntent = new Intent();
        getServerInfoIntent.setAction(OperationsService.ACTION_OAUTH2_GET_ACCESS_TOKEN);

        getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL,
                mOAuthTokenEndpointText.getText().toString().trim());

        getServerInfoIntent.putExtra(
                OperationsService.EXTRA_OAUTH2_QUERY_PARAMETERS,
                queryParameters);

        if (mOperationsServiceBinder != null) {
            //Log_OC.e(TAG, "getting access token..." );
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getServerInfoIntent);
        }
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
                normalizeUrl(mHostUrlInput.getText().toString(), mServerInfo.mIsSslConn))) {
            // check server again only if the user changed something in the field
            checkOcServer();
        } else {
            mOkButton.setEnabled(mServerIsValid);
            showRefreshButton(!mServerIsValid);
        }
    }


    private void checkOcServer() {
        String uri;
        if (mHostUrlInput != null) {
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
                uri = stripIndexPhpOrAppsFiles(uri, mHostUrlInput);
            }

            // Handle internationalized domain names
            try {
                uri = DisplayUtils.convertIdn(uri, true);
            } catch (IllegalArgumentException ex) {
                // Let Owncloud library check the error of the malformed URI
            }

            if (mHostUrlInput != null) {
                mServerStatusText = R.string.auth_testing_connection;
                mServerStatusIcon = R.drawable.progress_small;
                showServerStatus();
            }

            Intent getServerInfoIntent = new Intent();
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
            getServerInfoIntent.putExtra(
                    OperationsService.EXTRA_SERVER_URL,
                    normalizeUrlSuffix(uri)
            );

            if (mOperationsServiceBinder != null) {
                mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getServerInfoIntent);
            } else {
                Log_OC.e(TAG, "Server check tried with OperationService unbound!");
            }

        } else {
            mServerStatusText = 0;
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
                mServerInfo.mBaseUrl == null ||
                mServerInfo.mBaseUrl.length() == 0) {
            mServerStatusIcon = R.drawable.ic_alert;
            mServerStatusText = R.string.auth_wtf_reenter_URL;
            showServerStatus();
            mOkButton.setEnabled(false);
            return;
        }

        if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType()).
                equals(mAuthTokenType)) {

            startOauthorization();
        } else if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType)) {

            startSamlBasedFederatedSingleSignOnAuthorization();
        } else {
            checkBasicAuthorization(null, null);
        }

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
        IndeterminateProgressDialog dialog =
                IndeterminateProgressDialog.newInstance(R.string.auth_trying_to_login, true);
        dialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);

        /// validate credentials accessing the root folder
        OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBasicCredentials(username,
                password);
        accessRootFolder(credentials);
    }

    private void accessRootFolder(OwnCloudCredentials credentials) {
        mAsyncTask = new AuthenticatorAsyncTask(this);
        Object[] params = {mServerInfo.mBaseUrl, credentials};
        mAsyncTask.execute(params);
    }


    /**
     * Starts the OAuth 'grant type' flow to get an access token, with
     * a GET AUTHORIZATION request to the BUILT-IN authorization server.
     */
    private void startOauthorization() {
        // be gentle with the user
        mAuthStatusIcon = R.drawable.progress_small;
        mAuthStatusText = R.string.oauth_login_connection;
        showAuthStatus();

        // GET AUTHORIZATION request
        Uri uri = Uri.parse(mOAuthAuthEndpointText.getText().toString().trim());
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter(
                OAuth2Constants.KEY_RESPONSE_TYPE, getString(R.string.oauth2_response_type)
        );
        uriBuilder.appendQueryParameter(
                OAuth2Constants.KEY_REDIRECT_URI, getString(R.string.oauth2_redirect_uri)
        );
        uriBuilder.appendQueryParameter(
                OAuth2Constants.KEY_CLIENT_ID, getString(R.string.oauth2_client_id)
        );
        uriBuilder.appendQueryParameter(
                OAuth2Constants.KEY_SCOPE, getString(R.string.oauth2_scope)
        );
        uri = uriBuilder.build();
        Log_OC.d(TAG, "Starting browser to view " + uri.toString());
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }


    /**
     * Starts the Web Single Sign On flow to get access to the root folder
     * in the server.
     */
    private void startSamlBasedFederatedSingleSignOnAuthorization() {
        /// be gentle with the user
        mAuthStatusIcon = R.drawable.progress_small;
        mAuthStatusText = R.string.auth_connecting_auth_server;
        showAuthStatus();

        /// Show SAML-based SSO web dialog
        String targetUrl = mServerInfo.mBaseUrl
                + AccountUtils.getWebdavPath(mServerInfo.mVersion, mAuthTokenType);
        SamlWebViewDialog dialog = SamlWebViewDialog.newInstance(targetUrl, targetUrl);
        dialog.show(getSupportFragmentManager(), SAML_DIALOG_TAG);
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

        } else if (operation instanceof OAuth2GetAccessToken) {
            onGetOAuthAccessTokenFinish(result);

        } else if (operation instanceof GetRemoteUserInfoOperation) {
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
                    mAuthToken = "";
                    updateAuthStatusIconAndText(result);
                    showAuthStatus();
                    Log_OC.d(TAG, result.getLogMessage());
                } else {
                    try {
                        updateAccountAuthentication();
                        success = true;

                    } catch (AccountNotFoundException e) {
                        Log_OC.e(TAG, "Account " + mAccount + " was removed!", e);
                        Toast.makeText(this, R.string.auth_account_does_not_exist,
                                Toast.LENGTH_SHORT).show();
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

            if (webViewLoginMethod) {
                checkBasicAuthorization(webViewUser, webViewPassword);
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

        /// very special case (TODO: move to a common place for all the remote operations)
        if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result);
        }
    }


    private boolean authSupported(AuthenticationMethod authMethod) {
        return ((BASIC_TOKEN_TYPE.equals(mAuthTokenType) &&
                AuthenticationMethod.BASIC_HTTP_AUTH.equals(authMethod)) ||
                (OAUTH_TOKEN_TYPE.equals(mAuthTokenType) &&
                        AuthenticationMethod.BEARER_TOKEN.equals(authMethod)) ||
                (SAML_TOKEN_TYPE.equals(mAuthTokenType) &&
                        AuthenticationMethod.SAML_WEB_SSO.equals(authMethod))
        );
    }


    // TODO remove, if possible
    private String normalizeUrl(String url, boolean sslWhenUnprefixed) {

        if (url != null && url.length() > 0) {
            url = url.trim();
            if (!url.toLowerCase().startsWith(HTTP_PROTOCOL) &&
                    !url.toLowerCase().startsWith(HTTP_PROTOCOL)) {
                if (sslWhenUnprefixed) {
                    url = HTTPS_PROTOCOL + url;
                } else {
                    url = HTTP_PROTOCOL + url;
                }
            }

            url = normalizeUrlSuffix(url);
        }
        return (url != null ? url : "");
    }


    private String normalizeUrlSuffix(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = trimUrlWebdav(url);
        return url;
    }

    private String stripIndexPhpOrAppsFiles(String url, EditText mHostUrlInput) {
        if (url.endsWith("/index.php")) {
            url = url.substring(0, url.lastIndexOf("/index.php"));
            mHostUrlInput.setText(url);
        } else if (url.contains("/index.php/apps/")) {
            url = url.substring(0, url.lastIndexOf("/index.php/apps/"));
            mHostUrlInput.setText(url);
        }

        return url;
    }

    // TODO remove, if possible
    private String trimUrlWebdav(String url) {
        if (url.toLowerCase().endsWith(AccountUtils.WEBDAV_PATH_4_0_AND_LATER)) {
            url = url.substring(0, url.length() - AccountUtils.WEBDAV_PATH_4_0_AND_LATER.length());
        }
        return url;
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
                mServerStatusText = R.string.auth_secure_connection;
                break;

            case OK_NO_SSL:
            case OK:
                if (mHostUrlInput.getText().toString().trim().toLowerCase().startsWith(HTTP_PROTOCOL)) {
                    mServerStatusText = R.string.auth_connection_established;
                    mServerStatusIcon = R.drawable.ic_ok;
                } else {
                    mServerStatusText = R.string.auth_nossl_plain_ok_title;
                    mServerStatusIcon = R.drawable.ic_lock_open_white;
                }
                break;

            case NO_NETWORK_CONNECTION:
                mServerStatusIcon = R.drawable.no_network;
                mServerStatusText = R.string.auth_no_net_conn_title;
                break;

            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                mServerStatusText = R.string.auth_ssl_unverified_server_title;
                break;
            case BAD_OC_VERSION:
                mServerStatusText = R.string.auth_bad_oc_version_title;
                break;
            case WRONG_CONNECTION:
                mServerStatusText = R.string.auth_wrong_connection_title;
                break;
            case TIMEOUT:
                mServerStatusText = R.string.auth_timeout_title;
                break;
            case INCORRECT_ADDRESS:
                mServerStatusText = R.string.auth_incorrect_address_title;
                break;
            case SSL_ERROR:
                mServerStatusText = R.string.auth_ssl_general_error_title;
                break;
            case UNAUTHORIZED:
                mServerStatusText = R.string.auth_unauthorized;
                break;
            case HOST_NOT_AVAILABLE:
                mServerStatusText = R.string.auth_unknown_host_title;
                break;
            case INSTANCE_NOT_CONFIGURED:
                mServerStatusText = R.string.auth_not_configured_title;
                break;
            case FILE_NOT_FOUND:
                mServerStatusText = R.string.auth_incorrect_path_title;
                break;
            case OAUTH2_ERROR:
                mServerStatusText = R.string.auth_oauth_error;
                break;
            case OAUTH2_ERROR_ACCESS_DENIED:
                mServerStatusText = R.string.auth_oauth_error_access_denied;
                break;
            case UNHANDLED_HTTP_CODE:
            case UNKNOWN_ERROR:
                mServerStatusText = R.string.auth_unknown_error_title;
                break;
            case OK_REDIRECT_TO_NON_SECURE_CONNECTION:
                mServerStatusIcon = R.drawable.ic_lock_open_white;
                mServerStatusText = R.string.auth_redirect_non_secure_connection_title;
                break;
            case MAINTENANCE_MODE:
                mServerStatusText = R.string.auth_maintenance_mode;
                break;
            default:
                mServerStatusText = 0;
                mServerStatusIcon = 0;
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
                mAuthStatusText = R.string.auth_secure_connection;
                break;

            case OK_NO_SSL:
            case OK:
                if (mHostUrlInput.getText().toString().trim().toLowerCase().startsWith(HTTP_PROTOCOL)) {
                    mAuthStatusText = R.string.auth_connection_established;
                    mAuthStatusIcon = R.drawable.ic_ok;
                } else {
                    mAuthStatusText = R.string.auth_nossl_plain_ok_title;
                    mAuthStatusIcon = R.drawable.ic_lock_open_white;
                }
                break;

            case NO_NETWORK_CONNECTION:
                mAuthStatusIcon = R.drawable.no_network;
                mAuthStatusText = R.string.auth_no_net_conn_title;
                break;

            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                mAuthStatusText = R.string.auth_ssl_unverified_server_title;
                break;
            case BAD_OC_VERSION:
                mAuthStatusText = R.string.auth_bad_oc_version_title;
                break;
            case WRONG_CONNECTION:
                mAuthStatusText = R.string.auth_wrong_connection_title;
                break;
            case TIMEOUT:
                mAuthStatusText = R.string.auth_timeout_title;
                break;
            case INCORRECT_ADDRESS:
                mAuthStatusText = R.string.auth_incorrect_address_title;
                break;
            case SSL_ERROR:
                mAuthStatusText = R.string.auth_ssl_general_error_title;
                break;
            case UNAUTHORIZED:
                mAuthStatusText = R.string.auth_unauthorized;
                break;
            case HOST_NOT_AVAILABLE:
                mAuthStatusText = R.string.auth_unknown_host_title;
                break;
            case INSTANCE_NOT_CONFIGURED:
                mAuthStatusText = R.string.auth_not_configured_title;
                break;
            case FILE_NOT_FOUND:
                mAuthStatusText = R.string.auth_incorrect_path_title;
                break;
            case OAUTH2_ERROR:
                mAuthStatusText = R.string.auth_oauth_error;
                break;
            case OAUTH2_ERROR_ACCESS_DENIED:
                mAuthStatusText = R.string.auth_oauth_error_access_denied;
                break;
            case ACCOUNT_NOT_NEW:
                mAuthStatusText = R.string.auth_account_not_new;
                break;
            case ACCOUNT_NOT_THE_SAME:
                mAuthStatusText = R.string.auth_account_not_the_same;
                break;
            case UNHANDLED_HTTP_CODE:
            case UNKNOWN_ERROR:
                mAuthStatusText = R.string.auth_unknown_error_title;
                break;
            default:
                mAuthStatusText = 0;
                mAuthStatusIcon = 0;
        }
    }


    private void updateStatusIconFailUserName(int failedStatusText){
        mAuthStatusIcon = R.drawable.ic_alert;
        mAuthStatusText = failedStatusText;
    }

    private void updateFailedAuthStatusIconAndText(int failedStatusText){
        mAuthStatusIcon = R.drawable.ic_alert;
        mAuthStatusText = failedStatusText;
    }

    private void updateServerStatusIconNoRegularAuth() {
        mServerStatusIcon = R.drawable.ic_alert;
        mServerStatusText = R.string.auth_can_not_auth_against_server;
    }

    /**
     * Processes the result of the request for and access token send
     * to an OAuth authorization server.
     *
     * @param result Result of the operation.
     */
    private void onGetOAuthAccessTokenFinish(RemoteOperationResult result) {
        mWaitingForOpId = Long.MAX_VALUE;
        dismissDialog(WAIT_DIALOG_TAG);

        if (result.isSuccess()) {
            /// be gentle with the user
            IndeterminateProgressDialog dialog =
                    IndeterminateProgressDialog.newInstance(R.string.auth_trying_to_login, true);
            dialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);

            /// time to test the retrieved access token on the ownCloud server
            @SuppressWarnings("unchecked")
            Map<String, String> tokens = (Map<String, String>) (result.getData().get(0));
            mAuthToken = tokens.get(OAuth2Constants.KEY_ACCESS_TOKEN);
            Log_OC.d(TAG, "Got ACCESS TOKEN: " + mAuthToken);

            /// validate token accessing to root folder / getting session
            OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBearerCredentials(
                    mAuthToken);
            accessRootFolder(credentials);

        } else {
            updateAuthStatusIconAndText(result);
            showAuthStatus();
            Log_OC.d(TAG, "Access failed: " + result.getLogMessage());
        }
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
                    Toast.makeText(this, R.string.auth_account_does_not_exist,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            if (success) {
                finish();
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
            mAuthStatusText = 0;
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
            if (!webViewLoginMethod) {
                updateAuthStatusIconAndText(result);
                showAuthStatus();
            }
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

        if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType()).
                equals(mAuthTokenType)) {
            response.putString(AccountManager.KEY_AUTHTOKEN, mAuthToken);
            // the next line is necessary, notifications are calling directly to the 
            // AuthenticatorActivity to update, without AccountManager intervention
            mAccountMgr.setAuthToken(mAccount, mAuthTokenType, mAuthToken);

        } else if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType)) {

            response.putString(AccountManager.KEY_AUTHTOKEN, mAuthToken);
            // the next line is necessary; by now, notifications are calling directly to the 
            // AuthenticatorActivity to update, without AccountManager intervention
            mAccountMgr.setAuthToken(mAccount, mAuthTokenType, mAuthToken);

        } else {
            if (!webViewLoginMethod) {
                response.putString(AccountManager.KEY_AUTHTOKEN, mPasswordInput.getText().toString());
                mAccountMgr.setPassword(mAccount, mPasswordInput.getText().toString());
            } else {
                response.putString(AccountManager.KEY_AUTHTOKEN, webViewPassword);
                mAccountMgr.setPassword(mAccount, webViewPassword);
            }
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
    private boolean createAccount(RemoteOperationResult authResult) {
        /// create and save new ownCloud account
        boolean isOAuth = AccountTypeUtils.
                getAuthTokenTypeAccessToken(MainApp.getAccountType()).equals(mAuthTokenType);
        boolean isSaml = AccountTypeUtils.
                getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).equals(mAuthTokenType);

        String lastPermanentLocation = authResult.getLastPermanentLocation();
        if (lastPermanentLocation != null) {
            mServerInfo.mBaseUrl = AccountUtils.trimWebdavSuffix(lastPermanentLocation);
        }

        Uri uri = Uri.parse(mServerInfo.mBaseUrl);
        String username;
        if (!webViewLoginMethod) {
            username = mUsernameInput.getText().toString().trim();
        } else {
            username = webViewUser;
        }
        if (isOAuth) {
            username = "OAuth_user" + (new java.util.Random(System.currentTimeMillis())).nextLong();
        }
        String accountName = com.owncloud.android.lib.common.accounts.AccountUtils.
                buildAccountName(uri, username);
        Account newAccount = new Account(accountName, MainApp.getAccountType());
        if (AccountUtils.exists(newAccount, getApplicationContext())) {
            // fail - not a new account, but an existing one; disallow
            RemoteOperationResult result = new RemoteOperationResult(ResultCode.ACCOUNT_NOT_NEW);
            if (!webViewLoginMethod) {
                updateAuthStatusIconAndText(result);
                showAuthStatus();
            }
            Log_OC.d(TAG, result.getLogMessage());
            return false;

        } else {
            mAccount = newAccount;

            if (isOAuth || isSaml) {
                // with external authorizations, the password is never input in the app
                mAccountMgr.addAccountExplicitly(mAccount, "", null);
            } else {
                if (!webViewLoginMethod) {
                    mAccountMgr.addAccountExplicitly(
                            mAccount, mPasswordInput.getText().toString(), null
                    );
                } else {
                    mAccountMgr.addAccountExplicitly(
                            mAccount, webViewPassword, null
                    );
                }
            }

            // include account version with the new account
            mAccountMgr.setUserData(
                    mAccount,
                    Constants.KEY_OC_ACCOUNT_VERSION,
                    Integer.toString(AccountUtils.ACCOUNT_VERSION)
            );

            /// add the new account as default in preferences, if there is none already
            Account defaultAccount = AccountUtils.getCurrentOwnCloudAccount(this);
            if (defaultAccount == null) {
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(this).edit();
                editor.putString("select_oc_account", accountName);
                editor.commit();
            }

            /// prepare result to return to the Authenticator
            //  TODO check again what the Authenticator makes with it; probably has the same 
            //  effect as addAccountExplicitly, but it's not well done
            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, MainApp.getAccountType());
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
            intent.putExtra(AccountManager.KEY_USERDATA, username);
            if (isOAuth || isSaml) {
                mAccountMgr.setAuthToken(mAccount, mAuthTokenType, mAuthToken);
            }
            /// add user data to the new account; TODO probably can be done in the last parameter 
            //      addAccountExplicitly, or in KEY_USERDATA
            mAccountMgr.setUserData(
                    mAccount, Constants.KEY_OC_VERSION, mServerInfo.mVersion.getVersion()
            );
            mAccountMgr.setUserData(
                    mAccount, Constants.KEY_OC_BASE_URL, mServerInfo.mBaseUrl
            );
            if (authResult.getData() != null) {
                try {
                    UserInfo userInfo = (UserInfo) authResult.getData().get(0);
                    mAccountMgr.setUserData(
                            mAccount, Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName()
                    );
                } catch (ClassCastException c) {
                    Log_OC.w(TAG, "Couldn't get display name for " + username);
                }
            } else {
                Log_OC.w(TAG, "Couldn't get display name for " + username);
            }

            if (isSaml) {
                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO, "TRUE");
            } else if (isOAuth) {
                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2, "TRUE");
            }

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);

            return true;
        }
    }


    /**
     * Starts and activity to open the 'new account' page in the ownCloud web site
     *
     * @param view 'Account register' button
     */
    public void onRegisterClick(View view) {
        Intent register = new Intent(
                Intent.ACTION_VIEW, Uri.parse(getString(R.string.welcome_link_url))
        );
        setResult(RESULT_CANCELED);
        startActivity(register);
    }


    /**
     * Updates the content and visibility state of the icon and text associated
     * to the last check on the ownCloud server.
     */
    private void showServerStatus() {
        if (!webViewLoginMethod) {
            if (mServerStatusIcon == 0 && mServerStatusText == 0) {
                mServerStatusView.setVisibility(View.INVISIBLE);

            } else {
                mServerStatusView.setText(mServerStatusText);
                mServerStatusView.setCompoundDrawablesWithIntrinsicBounds(mServerStatusIcon, 0, 0, 0);
                mServerStatusView.setVisibility(View.VISIBLE);
            }
        }
    }


    /**
     * Updates the content and visibility state of the icon and text associated
     * to the interactions with the OAuth authorization server.
     */
    private void showAuthStatus() {
        if (!webViewLoginMethod) {
            if (mAuthStatusIcon == 0 && mAuthStatusText == 0) {
                mAuthStatusView.setVisibility(View.INVISIBLE);

            } else {
                mAuthStatusView.setText(mAuthStatusText);
                mAuthStatusView.setCompoundDrawablesWithIntrinsicBounds(mAuthStatusIcon, 0, 0, 0);
                mAuthStatusView.setVisibility(View.VISIBLE);
            }
        }
    }


    private void showRefreshButton(boolean show) {
        if (webViewLoginMethod) {
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
     * Called when the checkbox for OAuth authorization is clicked.
     *
     * Hides or shows the input fields for user & password.
     *
     * @param view 'View password' 'button'
     */
    public void onCheckClick(View view) {
        CheckBox oAuth2Check = (CheckBox) view;
        if (oAuth2Check.isChecked()) {
            mAuthTokenType = OAUTH_TOKEN_TYPE;
        } else {
            mAuthTokenType = BASIC_TOKEN_TYPE;
        }
        updateAuthenticationPreFragmentVisibility();
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

        } else if (actionId == EditorInfo.IME_ACTION_NEXT && inputField != null &&
                inputField.equals(mHostUrlInput) &&
                AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                        equals(mAuthTokenType)) {
            checkOcServer();
        }
        return false;   // always return false to grant that the software keyboard is hidden anyway
    }


    private abstract static class RightDrawableOnTouchListener implements OnTouchListener {

        private int fuzz = 75;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Drawable rightDrawable = null;
            if (view instanceof TextView) {
                Drawable[] drawables = ((TextView) view).getCompoundDrawables();
                if (drawables.length > 2) {
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


    private void getRemoteUserNameOperation(String sessionCookie) {

        Intent getUserNameIntent = new Intent();
        getUserNameIntent.setAction(OperationsService.ACTION_GET_USER_NAME);
        getUserNameIntent.putExtra(OperationsService.EXTRA_SERVER_URL, mServerInfo.mBaseUrl);
        getUserNameIntent.putExtra(OperationsService.EXTRA_COOKIE, sessionCookie);

        if (mOperationsServiceBinder != null) {
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getUserNameIntent);
        }
    }


    @Override
    public void onSsoFinished(String sessionCookie) {
        if (sessionCookie != null && sessionCookie.length() > 0) {
            Log_OC.d(TAG, "Successful SSO - time to save the account");
            mAuthToken = sessionCookie;
            getRemoteUserNameOperation(sessionCookie);
            Fragment fd = getSupportFragmentManager().findFragmentByTag(SAML_DIALOG_TAG);
            if (fd instanceof DialogFragment) {
                Dialog d = ((DialogFragment) fd).getDialog();
                if (d != null && d.isShowing()) {
                    d.dismiss();
                }
            }

        } else {
            // TODO - show fail
            Log_OC.d(TAG, "SSO failed");
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType) &&
                mHostUrlInput.hasFocus() && event.getAction() == MotionEvent.ACTION_DOWN) {
            checkOcServer();
        }
        return super.onTouchEvent(event);
    }


    /**
     * Show untrusted cert dialog
     */
    public void showUntrustedCertDialog(
            X509Certificate x509Certificate, SslError error, SslErrorHandler handler
    ) {
        // Show a dialog with the certificate info
        SslUntrustedCertDialog dialog;
        if (x509Certificate == null) {
            dialog = SslUntrustedCertDialog.newInstanceForEmptySslError(error, handler);
        } else {
            dialog = SslUntrustedCertDialog.
                    newInstanceForFullSslError(x509Certificate, error, handler);
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

    /**
     * Called from SslValidatorDialog when a new server certificate was correctly saved.
     */
    public void onSavedCertificate() {
        Fragment fd = getSupportFragmentManager().findFragmentByTag(SAML_DIALOG_TAG);
        if (fd == null) {
            // if SAML dialog is not shown, 
            // the SslDialog was shown due to an SSL error in the server check
            checkOcServer();
        }
    }

    /**
     * Called from SslValidatorDialog when a new server certificate could not be saved
     * when the user requested it.
     */
    @Override
    public void onFailedSavingCertificate() {
        dismissDialog(SAML_DIALOG_TAG);
        Toast.makeText(this, R.string.ssl_validator_not_saved, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCancelCertificate() {
        dismissDialog(SAML_DIALOG_TAG);
    }


    private void doOnResumeAndBound() {
        //Log_OC.e(TAG, "registering to listen for operation callbacks" );
        mOperationsServiceBinder.addOperationListener(AuthenticatorActivity.this, mHandler);
        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int) mWaitingForOpId, this);
        }

        if (!webViewLoginMethod && mHostUrlInput.getText() != null && mHostUrlInput.getText().length() > 0
                && !mServerIsChecked) {
            checkOcServer();
        }
    }


    private void dismissDialog(String dialogTag) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(dialogTag);
        if (frag instanceof DialogFragment) {
            DialogFragment dialog = (DialogFragment) frag;
            dialog.dismiss();
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

                doOnResumeAndBound();

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
     * @param webView Web view to emebd into the authentication dialog.
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
            Toast.makeText(
                    getApplicationContext(),
                    getText(R.string.saml_authentication_wrong_pass),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            mIsFirstAuthAttempt = false;
        }
    }

    /**
     * For retrieving the clicking on authentication cancel button
     */
    public void doNegativeAuthenticatioDialogClick() {
        mIsFirstAuthAttempt = true;
    }
}
