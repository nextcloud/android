/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 David Luhmer <david-dev@live.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.nextcloud.android.sso.InputStreamBinder;
import com.nextcloud.client.account.UserAccountManager;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class AccountManagerService extends Service {

    private InputStreamBinder mBinder;
    @Inject UserAccountManager accountManager;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(mBinder == null) {
            mBinder = new InputStreamBinder(getApplicationContext(), accountManager);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

}
