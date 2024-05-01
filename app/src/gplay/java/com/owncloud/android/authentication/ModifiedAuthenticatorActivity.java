/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication;

import android.os.Bundle;

import com.nextcloud.client.di.Injectable;
import com.owncloud.android.utils.GooglePlayUtils;

public class ModifiedAuthenticatorActivity extends AuthenticatorActivity implements Injectable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GooglePlayUtils.checkPlayServices(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GooglePlayUtils.checkPlayServices(this);
    }

}
