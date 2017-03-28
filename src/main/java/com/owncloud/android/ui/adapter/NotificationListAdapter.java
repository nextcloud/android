/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This Adapter populates a ListView with all notifications for an account within the app.
 */
public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder> {
    private List<Notification> mValues;
    private Context context;

    public NotificationListAdapter(Context context) {
        this.mValues = new ArrayList<>();
        this.context = context;
    }

    public void setNotificationItems(List<Notification> notificationItems) {
        mValues.clear();
        mValues.addAll(notificationItems);
        notifyDataSetChanged();
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        Notification notification = mValues.get(position);
        holder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context, notification.getDatetime().getTime()));
        holder.subject.setText(notification.getSubject());
        holder.message.setText(notification.getMessage());

        // Todo set proper action icon (to be clarified how to pick)
        holder.activityIcon.setImageResource(R.drawable.ic_notification);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView activityIcon;
        private final TextView subject;
        private final TextView message;
        private final TextView dateTime;

        private NotificationViewHolder(View itemView) {
            super(itemView);
            activityIcon = (ImageView) itemView.findViewById(R.id.activity_icon);
            subject = (TextView) itemView.findViewById(R.id.activity_subject);
            message = (TextView) itemView.findViewById(R.id.activity_message);
            dateTime = (TextView) itemView.findViewById(R.id.activity_datetime);
        }
    }
}