/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.services;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;
import java.util.ArrayList;

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
    protected Result onRunJob(Params params) {
        PersistableBundleCompat bundle = params.getExtras();

        String vCardFilePath = bundle.getString(VCARD_FILE_PATH, "");
        String accountName = bundle.getString(ACCOUNT_NAME, "");
        String accountType = bundle.getString(ACCOUNT_TYPE, "");
        int[] intArray = bundle.getIntArray(CHECKED_ITEMS_ARRAY);

        File file = new File(vCardFilePath);
        ArrayList<VCard> vCards = new ArrayList<>();

        try {
            ContactOperations operations = new ContactOperations(getContext(), accountName, accountType);
            vCards.addAll(Ezvcard.parse(file).all());

            for (int i = 0; i < intArray.length; i++ ){
                operations.insertContact(vCards.get(intArray[i]));
            }
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
        }

        return Result.SUCCESS;
    }
}
