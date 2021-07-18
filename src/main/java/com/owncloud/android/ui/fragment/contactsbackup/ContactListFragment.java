/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.files.downloader.Direction;
import com.nextcloud.client.files.downloader.DownloadRequest;
import com.nextcloud.client.files.downloader.Request;
import com.nextcloud.client.files.downloader.Transfer;
import com.nextcloud.client.files.downloader.TransferManagerConnection;
import com.nextcloud.client.files.downloader.TransferState;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ContactlistFragmentBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.events.VCardToggleEvent;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Photo;
import kotlin.Unit;

import static com.owncloud.android.ui.fragment.contactsbackup.ContactListFragment.getDisplayName;

/**
 * This fragment shows all contacts from a file and allows to import them.
 */
public class ContactListFragment extends FileFragment implements Injectable {
    public static final String TAG = ContactListFragment.class.getSimpleName();

    public static final String FILE_NAME = "FILE_NAME";
    public static final String USER = "USER";
    public static final String CHECKED_ITEMS_ARRAY_KEY = "CHECKED_ITEMS";

    private static final int SINGLE_ACCOUNT = 1;

    private ContactlistFragmentBinding binding;

    private ContactListAdapter contactListAdapter;
    private final List<VCard> vCards = new ArrayList<>();
    private OCFile ocFile;
    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject BackgroundJobManager backgroundJobManager;
    private TransferManagerConnection fileDownloader;

    public static ContactListFragment newInstance(OCFile file, User user) {
        ContactListFragment frag = new ContactListFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(FILE_NAME, file);
        arguments.putParcelable(USER, user);
        frag.setArguments(arguments);
        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_contact_list, menu);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = ContactlistFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setHasOptionsMenu(true);

        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        if (contactsPreferenceActivity != null) {
            ActionBar actionBar = contactsPreferenceActivity.getSupportActionBar();
            if (actionBar != null) {
                ThemeToolbarUtils.setColoredTitle(actionBar, R.string.actionbar_contacts_restore, getContext());
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            contactsPreferenceActivity.setDrawerIndicatorEnabled(false);
        }

        if (savedInstanceState == null) {
            contactListAdapter = new ContactListAdapter(accountManager, clientFactory, getContext(), vCards);
        } else {
            Set<Integer> checkedItems = new HashSet<>();
            int[] itemsArray = savedInstanceState.getIntArray(CHECKED_ITEMS_ARRAY_KEY);
            if (itemsArray != null) {
                for (int checkedItem : itemsArray) {
                    checkedItems.add(checkedItem);
                }
            }
            if (checkedItems.size() > 0) {
                onMessageEvent(new VCardToggleEvent(true));
            }
            contactListAdapter = new ContactListAdapter(accountManager, getContext(), vCards, checkedItems);
        }
        binding.contactlistRecyclerview.setAdapter(contactListAdapter);
        binding.contactlistRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));

        ocFile = getArguments().getParcelable(FILE_NAME);
        setFile(ocFile);
        User user = getArguments().getParcelable(USER);
        fileDownloader = new TransferManagerConnection(getActivity(), user);
        fileDownloader.registerTransferListener(this::onDownloadUpdate);
        fileDownloader.bind();
        if (!ocFile.isDown()) {
            Request request = new DownloadRequest(user, ocFile);
            fileDownloader.enqueue(request);
        } else {
            loadContactsTask.execute();
        }

        binding.contactlistRestoreSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAndAskForContactsWritePermission()) {
                    getAccountForImport();
                }
            }
        });

        binding.contactlistRestoreSelected.setTextColor(ThemeColorUtils.primaryAccentColor(getContext()));

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (fileDownloader != null) {
            fileDownloader.unbind();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(CHECKED_ITEMS_ARRAY_KEY, contactListAdapter.getCheckedIntArray());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(VCardToggleEvent event) {
        if (event.showRestoreButton) {
            binding.contactlistRestoreSelectedContainer.setVisibility(View.VISIBLE);
        } else {
            binding.contactlistRestoreSelectedContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        contactsPreferenceActivity.setDrawerIndicatorEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        if (loadContactsTask != null) {
            loadContactsTask.cancel(true);
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
            if (contactsPreferenceActivity != null) {
                contactsPreferenceActivity.onBackPressed();
            }
            retval = true;
        } else if (itemId == R.id.action_select_all) {
            item.setChecked(!item.isChecked());
            setSelectAllMenuItem(item, item.isChecked());
            contactListAdapter.selectAllFiles(item.isChecked());
            retval = true;
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    private void setLoadingMessage() {
        binding.loadingListContainer.setVisibility(View.VISIBLE);
    }

    private void setSelectAllMenuItem(MenuItem selectAll, boolean checked) {
        selectAll.setChecked(checked);
        if (checked) {
            selectAll.setIcon(R.drawable.ic_select_none);
        } else {
            selectAll.setIcon(R.drawable.ic_select_all);
        }
    }

    static class ContactItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView badge;
        private CheckedTextView name;

        ContactItemViewHolder(View itemView) {
            super(itemView);

            badge = itemView.findViewById(R.id.contactlist_item_icon);
            name = itemView.findViewById(R.id.contactlist_item_name);


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

    private void importContacts(ContactsAccount account) {
        backgroundJobManager.startImmediateContactsImport(account.name,
                                                          account.type,
                                                          getFile().getStoragePath(),
                                                          contactListAdapter.getCheckedIntArray());

        Snackbar
            .make(
                binding.contactlistRecyclerview,
                R.string.contacts_preferences_import_scheduled,
                Snackbar.LENGTH_LONG
                 )
            .show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    getActivity().finish();
                }
            }
        }, 1750);
    }

    private void getAccountForImport() {
        final ArrayList<ContactsAccount> contactsAccounts = new ArrayList<>();

        // add local one
        contactsAccounts.add(new ContactsAccount("Local contacts", null, null));

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

                    ContactsAccount account = new ContactsAccount(name, name, type);

                    if (!contactsAccounts.contains(account)) {
                        contactsAccounts.add(account);
                    }
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log_OC.d(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (contactsAccounts.size() == SINGLE_ACCOUNT) {
            importContacts(contactsAccounts.get(0));
        } else {
            ArrayAdapter adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, contactsAccounts);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.contactlist_account_chooser_title)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importContacts(contactsAccounts.get(which));
                    }
                }).show();
        }
    }

    private boolean checkAndAskForContactsWritePermission() {
        // check permissions
        if (!PermissionUtil.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                               PermissionUtil.PERMISSIONS_WRITE_CONTACTS);
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
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.contactlist_no_permission, Snackbar.LENGTH_LONG)
                                .show();
                        } else {
                            Toast.makeText(getContext(), R.string.contactlist_no_permission, Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                }
            }
        }
    }

    private class ContactsAccount {
        private String displayName;
        private String name;
        private String type;

        ContactsAccount(String displayName, String name, String type) {
            this.displayName = displayName;
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ContactsAccount) {
                ContactsAccount other = (ContactsAccount) obj;
                return this.name.equalsIgnoreCase(other.name) && this.type.equalsIgnoreCase(other.type);
            } else {
                return false;
            }
        }

        @NonNull
        @Override
        public String toString() {
            return displayName;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{displayName, name, type});
        }
    }

    private Unit onDownloadUpdate(Transfer download) {
        final Activity activity = getActivity();
        if (download.getState() == TransferState.COMPLETED && activity != null) {
            ocFile = download.getFile();
            loadContactsTask.execute();
        }
        return Unit.INSTANCE;
    }

    public static class VCardComparator implements Comparator<VCard> {
        @Override
        public int compare(VCard o1, VCard o2) {
            String contac1 = getDisplayName(o1);
            String contac2 = getDisplayName(o2);

            return contac1.compareToIgnoreCase(contac2);
        }


    }

    private AsyncTask<Void, Void, Boolean> loadContactsTask = new AsyncTask<Void, Void, Boolean>() {

        @Override
        protected void onPreExecute() {
            setLoadingMessage();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!isCancelled()) {
                File file = new File(ocFile.getStoragePath());
                try {
                    vCards.addAll(Ezvcard.parse(file).all());
                    Collections.sort(vCards, new VCardComparator());
                } catch (IOException e) {
                    Log_OC.e(TAG, "IO Exception: " + file.getAbsolutePath());
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (!isCancelled()) {
                binding.loadingListContainer.setVisibility(View.GONE);
                contactListAdapter.replaceVCards(vCards);
            }
        }
    };

    public static String getDisplayName(VCard vCard) {
        if (vCard.getFormattedName() != null) {
            return vCard.getFormattedName().getValue();
        } else if (vCard.getTelephoneNumbers() != null && vCard.getTelephoneNumbers().size() > 0) {
            return vCard.getTelephoneNumbers().get(0).getText();
        } else if (vCard.getEmails() != null && vCard.getEmails().size() > 0) {
            return vCard.getEmails().get(0).getValue();
        }

        return "";
    }
}

class ContactListAdapter extends RecyclerView.Adapter<ContactListFragment.ContactItemViewHolder> {
    private static final int SINGLE_SELECTION = 1;

    private List<VCard> vCards;
    private Set<Integer> checkedVCards;

    private Context context;

    private UserAccountManager accountManager;
    private ClientFactory clientFactory;

    ContactListAdapter(UserAccountManager accountManager, ClientFactory clientFactory, Context context,
                       List<VCard> vCards) {
        this.vCards = vCards;
        this.context = context;
        this.checkedVCards = new HashSet<>();
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
    }

    ContactListAdapter(UserAccountManager accountManager,
                       Context context,
                       List<VCard> vCards,
                       Set<Integer> checkedVCards) {
        this.vCards = vCards;
        this.context = context;
        this.checkedVCards = checkedVCards;
        this.accountManager = accountManager;
    }

    public int getCheckedCount() {
        if (checkedVCards != null) {
            return checkedVCards.size();
        } else {
            return 0;
        }
    }

    public void replaceVCards(List<VCard> vCards) {
        this.vCards = vCards;
        notifyDataSetChanged();
    }

    public int[] getCheckedIntArray() {
        int[] intArray;
        if (checkedVCards != null && checkedVCards.size() > 0) {
            intArray = new int[checkedVCards.size()];
            int i = 0;
            for (int position : checkedVCards) {
                intArray[i] = position;
                i++;
            }
            return intArray;
        } else {
            return new int[0];
        }
    }

    @NonNull
    @Override
    public ContactListFragment.ContactItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.contactlist_list_item, parent, false);

        return new ContactListFragment.ContactItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactListFragment.ContactItemViewHolder holder, final int position) {
        final int verifiedPosition = holder.getAdapterPosition();
        final VCard vcard = vCards.get(verifiedPosition);

        if (vcard != null) {

            setChecked(checkedVCards.contains(position), holder.getName());

            holder.getName().setText(getDisplayName(vcard));

            // photo
            if (vcard.getPhotos().size() > 0) {
                setPhoto(holder.getBadge(), vcard.getPhotos().get(0));
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

            holder.setVCardListener(v -> toggleVCard(holder, verifiedPosition));
        }
    }

    private void setPhoto(ImageView imageView, Photo firstPhoto) {
        String url = firstPhoto.getUrl();
        byte[] data = firstPhoto.getData();

        if (data != null && data.length > 0) {
            Bitmap thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
            RoundedBitmapDrawable drawable = BitmapUtils.bitmapToCircularBitmapDrawable(context.getResources(),
                                                                                        thumbnail);

            imageView.setImageDrawable(drawable);
        } else if (url != null) {
            SimpleTarget target = new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                    imageView.setImageDrawable(resource);
                }

                @Override
                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    super.onLoadFailed(e, errorDrawable);
                    imageView.setImageDrawable(errorDrawable);
                }
            };
            DisplayUtils.downloadIcon(accountManager,
                                      clientFactory,
                                      context,
                                      url,
                                      target,
                                      R.drawable.ic_user,
                                      imageView.getWidth(),
                                      imageView.getHeight());
        }
    }

    private void setChecked(boolean checked, CheckedTextView checkedTextView) {
        checkedTextView.setChecked(checked);

        if (checked) {
            checkedTextView.getCheckMarkDrawable()
                .setColorFilter(ThemeColorUtils.primaryColor(context), PorterDuff.Mode.SRC_ATOP);
        } else {
            checkedTextView.getCheckMarkDrawable().clearColorFilter();
        }
    }

    private void toggleVCard(ContactListFragment.ContactItemViewHolder holder, int verifiedPosition) {
        holder.getName().setChecked(!holder.getName().isChecked());

        if (holder.getName().isChecked()) {
            holder.getName().getCheckMarkDrawable().setColorFilter(ThemeColorUtils.primaryColor(context),
                                                                   PorterDuff.Mode.SRC_ATOP);

            checkedVCards.add(verifiedPosition);
            if (checkedVCards.size() == SINGLE_SELECTION) {
                EventBus.getDefault().post(new VCardToggleEvent(true));
            }
        } else {
            holder.getName().getCheckMarkDrawable().clearColorFilter();

            checkedVCards.remove(verifiedPosition);

            if (checkedVCards.isEmpty()) {
                EventBus.getDefault().post(new VCardToggleEvent(false));
            }
        }
    }

    @Override
    public int getItemCount() {
        return vCards.size();
    }

    public void selectAllFiles(boolean select) {
        checkedVCards = new HashSet<>();
        if (select) {
            for (int i = 0; i < vCards.size(); i++) {
                checkedVCards.add(i);
            }
        }

        if (checkedVCards.size() > 0) {
            EventBus.getDefault().post(new VCardToggleEvent(true));
        } else {
            EventBus.getDefault().post(new VCardToggleEvent(false));
        }

        notifyDataSetChanged();
    }

}
