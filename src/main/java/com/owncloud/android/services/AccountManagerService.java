package com.owncloud.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.nextcloud.android.sso.InputStreamBinder;

public class AccountManagerService extends Service {

    private InputStreamBinder mBinder;

    @Override
    public IBinder onBind(Intent intent) {
        if(mBinder == null) {
            mBinder = new InputStreamBinder(this);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

}