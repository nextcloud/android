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

package com.owncloud.android.ui.fragment.contactsbackup;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.events.VCardToggleEvent;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.PermissionUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
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

    public static final String CHECKED_ITEMS_ARRAY_KEY = "CHECKED_ITEMS";


    @BindView(R.id.contactlist_recyclerview)
    public RecyclerView recyclerView;

    @BindView(R.id.contactlist_restore_selected)
    public Button restoreContacts;

    private ContactListAdapter contactListAdapter;

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
        ButterKnife.bind(this, view);

        setHasOptionsMenu(true);

        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        contactsPreferenceActivity.getSupportActionBar().setTitle(R.string.actionbar_contacts_restore);
        contactsPreferenceActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        contactsPreferenceActivity.setDrawerIndicatorEnabled(false);

        ArrayList<VCard> vCards = new ArrayList<>();

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

        restoreContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkAndAskForContactsWritePermission()) {
                    getAccountForImport();
                }
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.contactlist_recyclerview);

        if (savedInstanceState == null) {
            contactListAdapter = new ContactListAdapter(getContext(), vCards);
        } else {
            Set<Integer> checkedItems = new HashSet<>();
            int[] itemsArray = savedInstanceState.getIntArray(CHECKED_ITEMS_ARRAY_KEY);
            for (int i = 0; i < itemsArray.length; i++) {
                checkedItems.add(itemsArray[i]);
            }
            if (checkedItems.size() > 0) {
                onMessageEvent(new VCardToggleEvent(true));
            }
            contactListAdapter = new ContactListAdapter(getContext(), vCards, checkedItems);
        }
        recyclerView.setAdapter(contactListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(CHECKED_ITEMS_ARRAY_KEY, contactListAdapter.getCheckedIntArray());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(VCardToggleEvent event) {
        if (event.showRestoreButton) {
            restoreContacts.setVisibility(View.VISIBLE);
            restoreContacts.setBackgroundColor(getResources().getColor(R.color.primary_button_background_color));
        } else {
            restoreContacts.setVisibility(View.GONE);
            restoreContacts.setBackgroundColor(getResources().getColor(R.color.standard_grey));
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        contactsPreferenceActivity.setDrawerIndicatorEnabled(true);
    }

    public void onResume() {
        super.onResume();
        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        contactsPreferenceActivity.setDrawerIndicatorEnabled(false);
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        switch (item.getItemId()) {
            case android.R.id.home:
                contactsPreferenceActivity.onBackPressed();
                retval = true;
                break;
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
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


        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(ContactsImportJob.ACCOUNT_NAME, account.name);
        bundle.putString(ContactsImportJob.ACCOUNT_TYPE, account.type);
        bundle.putString(ContactsImportJob.VCARD_FILE_PATH, getFile().getStoragePath());
        bundle.putIntArray(ContactsImportJob.CHECKED_ITEMS_ARRAY, contactListAdapter.getCheckedIntArray());

        new JobRequest.Builder(ContactsImportJob.TAG)
                .setExtras(bundle)
                .setExecutionWindow(3_000L, 10_000L)
                .setRequiresCharging(false)
                .setPersisted(false)
                .setUpdateCurrent(false)
                .build()
                .schedule();


        Snackbar.make(recyclerView, R.string.contacts_preferences_import_scheduled, Snackbar.LENGTH_LONG).show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().popBackStack();
            }
        }, 2500);
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
    private Set<Integer> checkedVCards;

    private Context context;

    ContactListAdapter(Context context, List<VCard> vCards) {
        this.vCards = vCards;
        this.context = context;
        this.checkedVCards = new HashSet<>();
    }

    ContactListAdapter(Context context, List<VCard> vCards,
                       Set<Integer> checkedVCards) {
        this.vCards = vCards;
        this.context = context;
        this.checkedVCards = checkedVCards;
    }


    public int getCheckedCount() {
        if (checkedVCards != null) {
            return checkedVCards.size();
        } else {
            return 0;
        }
    }

    public int[] getCheckedIntArray() {
        int[] intArray;
        if (checkedVCards != null && checkedVCards.size() > 0) {
            intArray = new int[checkedVCards.size()];
            int i = 0;
            for (int position: checkedVCards) {
                intArray[i] = position;
                i++;
            }
            return intArray;
        } else {
            intArray = new int[0];
            return intArray;
        }
    }

    @Override
    public ContactListFragment.ContactItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.contactlist_list_item, parent, false);

        return new ContactListFragment.ContactItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ContactListFragment.ContactItemViewHolder holder, final int position) {
        final VCard vcard = vCards.get(position);

        if (vcard != null) {

            if (checkedVCards.contains(position)) {
                holder.getName().setChecked(true);
            } else {
                holder.getName().setChecked(false);
            }
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
                        if (!checkedVCards.contains(position)) {
                            checkedVCards.add(position);
                        }
                        if (checkedVCards.size() == 1) {
                            EventBus.getDefault().post(new VCardToggleEvent(true));
                        }


                    } else {
                        if (checkedVCards.contains(position)) {
                            checkedVCards.remove(position);
                        }

                        if (checkedVCards.size() == 0) {
                            EventBus.getDefault().post(new VCardToggleEvent(false));
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return vCards.size();
    }

}
