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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.ContactsImportJob;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.PermissionUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private RecyclerView recyclerView;
    private Set<Integer> checkedVCards;

    public static ContactListFragment newInstance(OCFile file, Account account) {
        ContactListFragment frag = new ContactListFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(FILE_NAME, file);
        arguments.putParcelable(ACCOUNT, account);
        frag.setArguments(arguments);

        return frag;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.contactlist_fragment, null);
        setHasOptionsMenu(true);

        ArrayList<VCard> vCards = new ArrayList<>();
        checkedVCards = new HashSet<>();

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

        recyclerView = (RecyclerView) view.findViewById(R.id.contactlist_recyclerview);


        ContactListAdapter.OnVCardClickListener vCardClickListener = new ContactListAdapter.OnVCardClickListener() {
            private void setRestoreButton() {
                if (checkedVCards.size() > 0) {
                    restoreContacts.setEnabled(true);
                    restoreContacts.setBackgroundColor(getResources().getColor(R.color.primary_button_background_color));
                } else {
                    restoreContacts.setEnabled(false);
                    restoreContacts.setBackgroundColor(getResources().getColor(R.color.standard_grey));
                }
            }

            @Override
            public void onVCardCheck(int position) {
                checkedVCards.add(position);
                Log_OC.d(TAG, position + " checked");

                setRestoreButton();
            }

            @Override
            public void onVCardUncheck(int position) {
                checkedVCards.remove(position);
                Log_OC.d(TAG, position + " unchecked");

                setRestoreButton();
            }
        };

        ContactListAdapter contactListAdapter = new ContactListAdapter(getContext(), vCards, vCardClickListener);
        recyclerView.setAdapter(contactListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_sync_account).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(false);
        menu.findItem(R.id.action_switch_view).setVisible(false);
    }

    static class ContactItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView badge;
        private CheckedTextView name;

        ContactItemViewHolder(View itemView) {
            super(itemView);

            badge = (ImageView) itemView.findViewById(R.id.contactlist_item_icon);
            name = (CheckedTextView) itemView.findViewById(R.id.contactlist_item_name);

            itemView.setTag(this);
        }

        public void setVCardListener(View.OnClickListener onClickListener) {
            itemView.setOnClickListener(onClickListener);
        }

        public ImageView getBadge() {
            return badge;
        }

        public void setBadge(ImageView badge) {
            this.badge = badge;
        }

        public CheckedTextView getName() {
            return name;
        }

        public void setName(CheckedTextView name) {
            this.name = name;
        }
    }

    private void importContacts(ContactAccount account) {
        int[] intArray = new int[checkedVCards.size()];

        int i = 0;
        for (Integer checkedVCard : checkedVCards) {
            intArray[i] = checkedVCard;
            i++;
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


        Snackbar.make(recyclerView, R.string.contacts_preferences_import_scheduled, Snackbar.LENGTH_LONG).show();
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
        private String displayName;
        private String name;
        private String type;

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

class ContactListAdapter extends RecyclerView.Adapter<ContactListFragment.ContactItemViewHolder> {
    private List<VCard> vCards;
    private Context context;
    private OnVCardClickListener vCardClickListener;

    ContactListAdapter(Context context, List<VCard> vCards, OnVCardClickListener vCardClickListener) {
        this.vCards = vCards;
        this.context = context;
        this.vCardClickListener = vCardClickListener;
    }


    @Override
    public ContactListFragment.ContactItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.contactlist_list_item, parent, false);

        return new ContactListFragment.ContactItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ContactListFragment.ContactItemViewHolder holder, final int position) {
        final VCard vcard = vCards.get(holder.getAdapterPosition());

        if (vcard != null) {
            // name
            StructuredName name = vcard.getStructuredName();
            if (name != null) {
                String first = (name.getGiven() == null) ? "" : name.getGiven() + " ";
                String last = (name.getFamily() == null) ? "" : name.getFamily();
                holder.getName().setText(first + last);
            } else {
                holder.getName().setText("");
            }

            // photo
            if (vcard.getPhotos().size() > 0) {
                byte[] data = vcard.getPhotos().get(0).getData();

                Bitmap thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
                RoundedBitmapDrawable drawable = BitmapUtils.bitmapToCircularBitmapDrawable(context.getResources(),
                        thumbnail);

                holder.getBadge().setImageDrawable(drawable);
            } else {
                try {
                    holder.getBadge().setImageDrawable(
                            TextDrawable.createNamedAvatar(
                                    holder.getName().getText().toString(),
                                    context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius)
                            )
                    );
                } catch (Exception e) {
                    holder.getBadge().setImageResource(R.drawable.ic_user);
                }
            }

            // Checkbox
            holder.setVCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.getName().setChecked(!holder.getName().isChecked());

                    if (holder.getName().isChecked()) {
                        vCardClickListener.onVCardCheck(holder.getAdapterPosition());
                    } else {
                        vCardClickListener.onVCardUncheck(holder.getAdapterPosition());
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return vCards.size();
    }

    interface OnVCardClickListener {
        void onVCardCheck(int position);

        void onVCardUncheck(int position);
    }
}
