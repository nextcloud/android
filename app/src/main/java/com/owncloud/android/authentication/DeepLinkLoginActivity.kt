package com.owncloud.android.authentication

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R

class DeepLinkLoginActivity : AuthenticatorActivity(), Injectable {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!resources.getBoolean(R.bool.multiaccount_support) &&
            accountManager.accounts.size == 1
        ) {
            Toast.makeText(this, R.string.no_mutliple_accounts_allowed, Toast.LENGTH_LONG).show()
            return
        }

        setContentView(R.layout.deep_link_login)

        intent.data?.let {
            try {
                val prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/"
                val loginUrlInfo = parseLoginDataUrl(prefix, it.toString())
                val loginText = findViewById<TextView>(R.id.loginInfo)
                loginText.text = String.format(
                    getString(R.string.direct_login_text), loginUrlInfo.username,
                    loginUrlInfo.serverAddress
                )
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, R.string.direct_login_failed, Toast.LENGTH_LONG).show()
            }
        }
    }
}
