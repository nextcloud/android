/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment.contactsbackup;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ContactlistListItemBinding;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.events.VCardToggleEvent;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.recyclerview.widget.RecyclerView;
import ezvcard.VCard;
import ezvcard.property.Photo;

import static com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment.getDisplayName;

class ContactListAdapter extends RecyclerView.Adapter<ContactItemViewHolder> {
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
    public ContactItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactItemViewHolder(ContactlistListItemBinding.inflate(LayoutInflater.from(parent.getContext()),
                                                                            parent,
                                                                            false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactItemViewHolder holder, final int position) {
        final int verifiedPosition = holder.getAdapterPosition();
        final VCard vcard = vCards.get(verifiedPosition);

        if (vcard != null) {

            setChecked(checkedVCards.contains(position), holder.binding.name);

            holder.binding.name.setText(getDisplayName(vcard));

            // photo
            if (vcard.getPhotos().size() > 0) {
                setPhoto(holder.binding.icon, vcard.getPhotos().get(0));
            } else {
                try {
                    holder.binding.icon.setImageDrawable(
                        TextDrawable.createNamedAvatar(
                            holder.binding.name.getText().toString(),
                            context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius)
                                                      )
                                                        );
                } catch (Exception e) {
                    holder.binding.icon.setImageResource(R.drawable.ic_user);
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

    private void toggleVCard(ContactItemViewHolder holder, int verifiedPosition) {
        holder.binding.name.setChecked(!holder.binding.name.isChecked());

        if (holder.binding.name.isChecked()) {
            holder.binding.name.getCheckMarkDrawable().setColorFilter(ThemeColorUtils.primaryColor(context),
                                                                      PorterDuff.Mode.SRC_ATOP);

            checkedVCards.add(verifiedPosition);
            if (checkedVCards.size() == SINGLE_SELECTION) {
                EventBus.getDefault().post(new VCardToggleEvent(true));
            }
        } else {
            holder.binding.name.getCheckMarkDrawable().clearColorFilter();

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

    public boolean isEmpty() {
        return getItemCount() == 0;
    }
}
