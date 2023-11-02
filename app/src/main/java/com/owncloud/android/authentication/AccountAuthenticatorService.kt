/**
 * ownCloud Android client application
 *
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.authentication

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AccountAuthenticatorService : Service() {
    private var mAuthenticator: AccountAuthenticator? = null

    override fun onCreate() {
        super.onCreate()
        mAuthenticator = AccountAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mAuthenticator?.iBinder
    }
}
