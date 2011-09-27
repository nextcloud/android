package eu.alefzero.owncloud.syncadapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.accounts.AccountManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;

import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.webdav.HttpPropFind;

import android.accounts.Account;
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

    private static final String TAG = "ContactSyncAdapter";

    public ContactSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public synchronized void onPerformSync(
            Account account, 
            Bundle extras, 
            String authority, 
            ContentProviderClient provider, 
            SyncResult syncResult) {

        this.setAccount(account);
        this.setContentProvider(provider);

        // TODO find all contacts on ownCloud that not synced or the sync date is behind than the last sync date
        Cursor cursor = getContacts();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String lookup = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                Log.d(TAG, "Found Contact id: " + id + " with lookupkey: "+lookup);

                try {
                    FileInputStream fis = getContactVcard(lookup);
                    
                    HttpPut query = new HttpPut(
                        getUri() + 
                        "/addressbooks/"+
                        getAccount().name.split("@")[0]+
                        "/default/"+
                        lookup+
                        ".vcf"
                    );
                    
                    byte[] b = new byte[fis.available()];
                    fis.read(b);
                    query.setEntity(new ByteArrayEntity(b));
                    HttpResponse response = fireRawRequest(query);
                    
                    if(201 != response.getStatusLine().getStatusCode()) {
                        syncResult.stats.numIoExceptions++;
                    }
                    // TODO make a webdav request based on the stream
                    // TODO send request to the ownCloud server
                    // TODO mark the current contact as synced - where to store?
                    fis.close();
                } catch (IOException e) {
                    syncResult.stats.numIoExceptions++;
                } catch (OperationCanceledException e) {
                    //TODO maybe to a better break here
                    return;
                } catch (AuthenticatorException e) {
                    syncResult.stats.numAuthExceptions++;
                }
            }
        }

    }

    protected Uri getUri() {
        Uri uri = Uri.parse(this.getAccountManager().getUserData(getAccount(), AccountAuthenticator.KEY_CONTACT_URL));
        return uri;
    }

   /**
     * Returns the vCard based on the LookupKey for Contact as Stream 
     * 
     * @param lookupKey
     * @return
     * @throws IOException
     */
    private FileInputStream getContactVcard(String lookupKey) throws IOException {
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        AssetFileDescriptor fd = getContext().getContentResolver().openAssetFileDescriptor(uri, "r");
        return fd.createInputStream();
    }

    /**
     * Obtains the contact list.
     *
     * @return A cursor for for accessing the contact list.
     */
    private Cursor getContacts()
    {
        // Run query
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY
        };

        boolean showInvisible = false;
        String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '" + 
                (showInvisible ? "0" : "1") + "'";
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.Contacts._ID + " DESC";

        return getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }



}
