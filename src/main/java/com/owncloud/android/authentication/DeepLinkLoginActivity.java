package com.owncloud.android.authentication;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.ThemeUtils;

public class DeepLinkLoginActivity extends AuthenticatorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.deep_link_login);

        Uri data = getIntent().getData();

        if (data != null) {
            String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
            LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, data.toString());

            TextView loginText = findViewById(R.id.loginInfo);
            loginText.setTextColor(ThemeUtils.fontColor(this));
            loginText.setText(String.format("Login with %1$s to %2$s", loginUrlInfo.username,
                                            loginUrlInfo.serverAddress));
        }
    }
}
