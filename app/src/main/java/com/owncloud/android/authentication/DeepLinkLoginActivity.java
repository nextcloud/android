package com.owncloud.android.authentication;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;

public class DeepLinkLoginActivity extends AuthenticatorActivity implements Injectable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getResources().getBoolean(R.bool.multiaccount_support) &&
            accountManager.getAccounts().length == 1) {
            Snackbar.make(new View(this), R.string.no_mutliple_accounts_allowed, Snackbar.LENGTH_LONG).show();
            return;
        }

        setContentView(R.layout.deep_link_login);

        Uri data = getIntent().getData();

        if (data != null) {
            try {
                String prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/";
                LoginUrlInfo loginUrlInfo = parseLoginDataUrl(prefix, data.toString());

                TextView loginText = findViewById(R.id.loginInfo);
                loginText.setText(String.format(getString(R.string.direct_login_text), loginUrlInfo.username,
                                                loginUrlInfo.serverAddress));
            } catch (IllegalArgumentException e) {
                Snackbar.make(new View(this), R.string.direct_login_failed, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
