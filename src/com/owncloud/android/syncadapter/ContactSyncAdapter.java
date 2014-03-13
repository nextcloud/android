/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.syncadapter;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

public class ContactSyncAdapter extends AbstractOwnCloudSyncAdapter {
    private String mAddrBookUri;

    public ContactSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mAddrBookUri = null;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        setAccount(account);
        setContentProviderClient(provider);
        Cursor c = getLocalContacts(false);
        if (c.moveToFirst()) {
            do {
                String lookup = c.getString(c
                        .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                String a = getAddressBookUri();
                String uri = a + lookup + ".vcf";
                FileInputStream f;
                try {
                    f = getContactVcard(lookup);
                    HttpPut query = new HttpPut(uri);
                    byte[] b = new byte[f.available()];
                    f.read(b);
                    query.setEntity(new ByteArrayEntity(b));
                    fireRawRequest(query);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (OperationCanceledException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } while (c.moveToNext());
            // } while (c.moveToNext());
        }

    }

    private String getAddressBookUri() {
        if (mAddrBookUri != null)
            return mAddrBookUri;

        AccountManager am = getAccountManager();
        @SuppressWarnings("deprecation")
        String uri = am.getUserData(getAccount(),
                Constants.KEY_OC_URL).replace(
                AccountUtils.WEBDAV_PATH_2_0, AccountUtils.CARDDAV_PATH_2_0);
        uri += "/addressbooks/"
                + getAccount().name.substring(0,
                        getAccount().name.lastIndexOf('@')) + "/default/";
        mAddrBookUri = uri;
        return uri;
    }

    private FileInputStream getContactVcard(String lookupKey)
            throws IOException {
        Uri uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        AssetFileDescriptor fd = getContext().getContentResolver()
                .openAssetFileDescriptor(uri, "r");
        return fd.createInputStream();
    }

    private Cursor getLocalContacts(boolean include_hidden_contacts) {
        return getContext().getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[] { ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY },
                ContactsContract.Contacts.IN_VISIBLE_GROUP + " = ?",
                new String[] { (include_hidden_contacts ? "0" : "1") },
                ContactsContract.Contacts._ID + " DESC");
    }

}
