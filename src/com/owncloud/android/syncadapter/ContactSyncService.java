package com.owncloud.android.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ContactSyncService extends Service {
    private static final Object syncAdapterLock = new Object();
    private static AbstractOwnCloudSyncAdapter mSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (mSyncAdapter == null) {
                mSyncAdapter = new ContactSyncAdapter(getApplicationContext(),
                        true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mSyncAdapter.getSyncAdapterBinder();
    }

}
