/*
 *  Nextcloud SingleSignOn
 *
 *  @author David Luhmer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
