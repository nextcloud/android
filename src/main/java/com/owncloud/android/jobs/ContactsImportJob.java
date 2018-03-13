/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.contactsbackup.ContactListFragment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import third_parties.ezvcard_android.ContactOperations;

/**
 * Job to import contacts
 */

public class ContactsImportJob extends Job {
    public static final String TAG = "ContactsImportJob";

    public static final String ACCOUNT_TYPE = "account_type";
    public static final String ACCOUNT_NAME = "account_name";
    public static final String VCARD_FILE_PATH = "vcard_file_path";
    public static final String CHECKED_ITEMS_ARRAY = "checked_items_array";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        PersistableBundleCompat bundle = params.getExtras();

        String vCardFilePath = bundle.getString(VCARD_FILE_PATH, "");
        String accountName = bundle.getString(ACCOUNT_NAME, "");
        String accountType = bundle.getString(ACCOUNT_TYPE, "");
        int[] intArray = bundle.getIntArray(CHECKED_ITEMS_ARRAY);

        File file = new File(vCardFilePath);
        ArrayList<VCard> vCards = new ArrayList<>();

        Cursor cursor = null;
        try {
            ContactOperations operations = new ContactOperations(getContext(), accountName, accountType);
            vCards.addAll(Ezvcard.parse(file).all());
            Collections.sort(vCards, new ContactListFragment.VCardComparator());
            cursor = getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
                    null, null, null);

            TreeMap<VCard, Long> ownContactList = new TreeMap<>(new ContactListFragment.VCardComparator());
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    VCard vCard = getContactFromCursor(cursor);
                    if (vCard != null) {
                        ownContactList.put(vCard, cursor.getLong(cursor.getColumnIndex("NAME_RAW_CONTACT_ID")));
                    }
                    cursor.moveToNext();
                }
            }


            for (int i = 0; i < intArray.length; i++) {
                VCard vCard = vCards.get(intArray[i]);
                if (ContactListFragment.getDisplayName(vCard).length() != 0) {
                    if (!ownContactList.containsKey(vCard)) {
                        operations.insertContact(vCard);
                    } else {
                        operations.updateContact(vCard, ownContactList.get(vCard));
                    }
                } else {
                    operations.insertContact(vCard); //Insert All the contacts without name
                }
            }
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return Result.SUCCESS;
    }

    private VCard getContactFromCursor(Cursor cursor) {
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        VCard vCard = null;
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            ArrayList<VCard> vCardList = new ArrayList<>();
            vCardList.addAll(Ezvcard.parse(inputStream).all());
            if (vCardList.size() > 0) {
                vCard = vCardList.get(0);
            }

        } catch (IOException e) {
            Log_OC.d(TAG, e.getMessage());
        }
        return vCard;
    }


}
