/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
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

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.StringPair;
import com.owncloud.android.lib.common.UserInfo;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This Adapter populates a RecyclerView with the user information.
 */


public class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {

    private UserInfo userInfo;
    private Context context;

    private List<StringPair> stringPairs;

    public UserInfoAdapter(UserInfo userInfo, Context context) {
        this.userInfo = userInfo;
        this.context = context;

        stringPairs = new ArrayList<>();
        getRelevantInformation();
    }

    @Override
    public int getItemViewType(int position) {
        if (position % 2 == 0) {
            return 0;
        } else {
            return 1;
        }

    }

    @Override
    public UserInfoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.user_info_list_item, parent, false);

        if (context != null) {
            if (viewType == 0) {
                itemView.setBackgroundColor(context.getResources().getColor(R.color.white));
            } else {
                itemView.setBackgroundColor(context.getResources().getColor(R.color.white2));
            }
        }
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(UserInfoAdapter.ViewHolder holder, int position) {
        StringPair currentPair = stringPairs.get(position);
        holder.attributeHeadlineTextView.setText(currentPair.first);
        holder.attributeValueTextView.setText(currentPair.second);
    }

    @Override
    public int getItemCount() {
        return stringPairs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.attribute_headline_tv)
        public TextView attributeHeadlineTextView;
        @BindView(R.id.attribute_value_tv)
        public TextView attributeValueTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private void getRelevantInformation() {
        if (context != null) {
            if (!TextUtils.isEmpty(userInfo.getDisplayName())) {
                stringPairs.add(new StringPair(context.getResources().getString(R.string.user_info_full_name),
                        userInfo.getDisplayName()));
            }

            if (!TextUtils.isEmpty((userInfo.getEmail()))) {
                stringPairs.add(new StringPair(context.getResources().getString(R.string.user_info_email),
                        userInfo.getEmail()));
            }

            if (!TextUtils.isEmpty(userInfo.getPhone())) {
                stringPairs.add(new StringPair(context.getResources().getString(R.string.user_info_phone),
                        userInfo.getPhone()));
            }

            if (!TextUtils.isEmpty(userInfo.getAddress())) {
                stringPairs.add(new StringPair(context.getResources().getString(R.string.user_info_address),
                        userInfo.getAddress()));
            }

            if (!TextUtils.isEmpty(userInfo.getWebpage())) {
                stringPairs.add(new StringPair(context.getResources().getString(R.string.user_info_website),
                        userInfo.getWebpage()));
            }

            if (!TextUtils.isEmpty(userInfo.getTwitter())) {
                stringPairs.add(new StringPair(context.getResources().getString(R.string.user_info_twitter),
                        userInfo.getTwitter()));
            }
        }
    }

}
