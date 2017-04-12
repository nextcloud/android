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

package com.owncloud.android.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.ContactsImportJob;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.PermissionUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.StructuredName;

/**
 * This fragment shows all contacts from a file and allows to import them.
 */

public class ContactListFragment extends FileFragment {
    public static final String TAG = ContactListFragment.class.getSimpleName();

    public static final String FILE_NAME = "FILE_NAME";
    public static final String ACCOUNT = "ACCOUNT";

    private ListView listView;
    private ArrayList<VCard> vCards;

    public static ContactListFragment newInstance(OCFile file, Account account) {
        ContactListFragment frag = new ContactListFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(FILE_NAME, file);
        arguments.putParcelable(ACCOUNT, account);
        frag.setArguments(arguments);

        return frag;
    }

    public ContactListFragment() {
        super();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.contactlist_fragment, null);

        vCards = new ArrayList<>();

        try {
            OCFile ocFile = getArguments().getParcelable(FILE_NAME);
            setFile(ocFile);
            Account account = getArguments().getParcelable(ACCOUNT);

            if (!ocFile.isDown()) {
                Intent i = new Intent(getContext(), FileDownloader.class);
                i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
                i.putExtra(FileDownloader.EXTRA_FILE, ocFile);
                getContext().startService(i);
            } else {
                File file = new File(ocFile.getStoragePath());
                vCards.addAll(Ezvcard.parse(file).all());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Button restoreContacts = (Button) view.findViewById(R.id.contactlist_restore_selected);
        restoreContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkAndAskForContactsWritePermission()) {
                    getAccountForImport();
                }
            }
        });

        ContactListAdapter contactListAdapter = new ContactListAdapter(getContext(), vCards);

        listView = (ListView) view.findViewById(R.id.contactlist_listview);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(contactListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CheckedTextView tv = (CheckedTextView) view.findViewById(R.id.contactlist_item_name);

                if (listView.getCheckedItemPositions().get(position)) {
                    tv.setChecked(true);
                } else {
                    listView.getCheckedItemPositions().delete(position);
                    tv.setChecked(false);
                }

                if (listView.getCheckedItemPositions().size() > 0) {
                    restoreContacts.setEnabled(true);
                    restoreContacts.setBackgroundColor(getResources().getColor(R.color.primary_button_background_color));
                } else {
                    restoreContacts.setEnabled(false);
                    restoreContacts.setBackgroundColor(getResources().getColor(R.color.standard_grey));
                }
            }
        });

        return view;
    }

    static class ContactItemViewHolder {
        QuickContactBadge badge;
        TextView name;
    }

    private void importContacts(ContactAccount account) {
        SparseBooleanArray checkedArray = listView.getCheckedItemPositions();
        int[] intArray = new int[vCards.size()];

        for (int i = 0; i < vCards.size(); i++) {
            if (checkedArray.get(i)) {
                intArray[i] = 1;
            }
        }

        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(ContactsImportJob.ACCOUNT_NAME, account.name);
        bundle.putString(ContactsImportJob.ACCOUNT_TYPE, account.type);
        bundle.putString(ContactsImportJob.VCARD_FILE_PATH, getFile().getStoragePath());
        bundle.putIntArray(ContactsImportJob.CHECKED_ITEMS_ARRAY, intArray);

        new JobRequest.Builder(ContactsImportJob.TAG)
                .setExtras(bundle)
                .setExecutionWindow(3_000L, 10_000L)
                .setRequiresCharging(false)
                .setPersisted(false)
                .setUpdateCurrent(false)
                .build()
                .schedule();


        Snackbar.make(listView, R.string.contacts_preferences_import_scheduled, Snackbar.LENGTH_LONG).show();
    }

    private void getAccountForImport() {
        final ArrayList<ContactAccount> accounts = new ArrayList<>();

        // add local one
        accounts.add(new ContactAccount("Local contacts", null, null));

        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE},
                    null,
                    null,
                    null);

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
                    String type = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

                    ContactAccount account = new ContactAccount(name, name, type);

                    if (!accounts.contains(account)) {
                        accounts.add(account);
                    }
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log_OC.d(TAG, e.getMessage());
        } finally {
            cursor.close();
        }

        if (accounts.size() == 1) {
            importContacts(accounts.get(0));
        } else {

            ArrayAdapter adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, accounts);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.contactlist_account_chooser_title)
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            importContacts(accounts.get(which));
                        }
                    }).show();
        }
    }

    private boolean checkAndAskForContactsWritePermission() {
        // check permissions
        if (!PermissionUtil.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS)) {
            PermissionUtil.requestWriteContactPermission(this);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.PERMISSIONS_WRITE_CONTACTS) {
            for (int index = 0; index < permissions.length; index++) {
                if (Manifest.permission.WRITE_CONTACTS.equalsIgnoreCase(permissions[index])) {
                    if (grantResults[index] >= 0) {
                        getAccountForImport();
                    } else {
                        Snackbar.make(getView(), R.string.contactlist_no_permission, Snackbar.LENGTH_LONG).show();
                    }
                    break;
                }
            }
        }
    }

    private class ContactAccount {
        String displayName;
        String name;
        String type;

        ContactAccount(String displayName, String name, String type) {
            this.displayName = displayName;
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ContactAccount) {
                ContactAccount other = (ContactAccount) obj;
                return this.name.equalsIgnoreCase(other.name) && this.type.equalsIgnoreCase(other.type);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}

class ContactListAdapter extends ArrayAdapter<VCard> {
    private List<VCard> vCards;

    ContactListAdapter(Context context, List<VCard> vCards) {
        super(context, 0, R.id.contactlist_item_name, vCards);

        this.vCards = vCards;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ContactListFragment.ContactItemViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.contactlist_list_item, parent, false);
            viewHolder = new ContactListFragment.ContactItemViewHolder();

            viewHolder.badge = (QuickContactBadge) convertView.findViewById(R.id.contactlist_item_icon);
            viewHolder.name = (TextView) convertView.findViewById(R.id.contactlist_item_name);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ContactListFragment.ContactItemViewHolder) convertView.getTag();
        }

        VCard vcard = vCards.get(position);

        if (vcard != null) {
            // photo
            if (vcard.getPhotos().size() > 0) {
                byte[] data = vcard.getPhotos().get(0).getData();

                Drawable drawable = new BitmapDrawable(BitmapFactory.decodeByteArray(data, 0, data.length));

                viewHolder.badge.setImageDrawable(drawable);
            } else {
                viewHolder.badge.setImageToDefault();
            }

            // name
            StructuredName name = vcard.getStructuredName();
            String first = (name.getGiven() == null) ? "" : name.getGiven() + " ";
            String last = (name.getFamily() == null) ? "" : name.getFamily();
            viewHolder.name.setText(first + last);
        }

        return convertView;
    }
}
