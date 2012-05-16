package eu.alefzero.owncloud.syncadapter;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
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
import android.util.Log;

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
        setContentProvider(provider);
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
                    HttpResponse response = fireRawRequest(query);
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
        String uri = am.getUserData(getAccount(),
                AccountAuthenticator.KEY_OC_URL).replace(
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
