/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication

import android.os.Bundle
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils

class DeepLinkLoginActivity :
    AuthenticatorActivity(),
    Injectable {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MDMConfig.multiAccountSupport(this) && accountManager.accounts.size == 1) {
            DisplayUtils.showSnackMessage(this, R.string.no_mutliple_accounts_allowed)
            return
        }

        setContentView(R.layout.deep_link_login)

        intent.data?.let {
            try {
                val prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/"
                val loginUrlInfo = parseLoginDataUrl(prefix, it.toString())

                intent.data = null

                loginDialog?.showDeepLinkDialog(loginUrlInfo)
            } catch (_: IllegalArgumentException) {
                DisplayUtils.showSnackMessage(this, R.string.direct_login_failed)
            }
        }
    }
}
