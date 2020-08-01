/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * @author Tobias Kaminsky
 *
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.interfaces.VersionListInterface;
import com.owncloud.android.utils.DisplayUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ActivityAndVersionListAdapter extends ActivityListAdapter {

    private static final int VERSION_TYPE = 102;
    private VersionListInterface.View versionListInterface;

    public ActivityAndVersionListAdapter(
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ActivityListInterface activityListInterface,
        VersionListInterface.View versionListInterface,
        FileDataStorageManager storageManager,
        OCCapability capability,
        ClientFactory clientFactory
    ) {
        super(context, currentAccountProvider, activityListInterface, storageManager, capability, clientFactory, true);

        this.versionListInterface = versionListInterface;
    }

    public void setActivityAndVersionItems(List<Object> items, NextcloudClient newClient, boolean clear) {
        if (clear) {
            values.clear();
            Collections.sort(items, (o1, o2) -> {
                long o1Date;
                long o2Date;
                if (o1 instanceof Activity) {
                    o1Date = ((Activity) o1).getDatetime().getTime();
                } else {
                    o1Date = ((FileVersion) o1).getModifiedTimestamp();
                }

                if (o2 instanceof Activity) {
                    o2Date = ((Activity) o2).getDatetime().getTime();
                } else {
                    o2Date = ((FileVersion) o2).getModifiedTimestamp();
                }

                return -1 * Long.compare(o1Date, o2Date);
            });
        }

        String sTime = "";
        for (Object item : items) {
            String time;

            if (item instanceof Activity) {
                Activity activity = (Activity) item;
                if (activity.getDatetime() != null) {
                    time = getHeaderDateString(context, activity.getDatetime().getTime()).toString();
                } else if (activity.getDate() != null) {
                    time = getHeaderDateString(context, activity.getDate().getTime()).toString();
                } else {
                    time = context.getString(R.string.date_unknown);
                }
            } else {
                FileVersion version = (FileVersion) item;
                time = getHeaderDateString(context, version.getModifiedTimestamp()).toString();
            }

            if (sTime.equalsIgnoreCase(time)) {
                values.add(item);
            } else {
                sTime = time;
                values.add(sTime);
                values.add(item);
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VERSION_TYPE:
                View versionView = LayoutInflater.from(parent.getContext()).inflate(R.layout.version_list_item,
                        parent, false);
                return new VersionViewHolder(versionView);
            default:
                return super.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VersionViewHolder) {
            final VersionViewHolder versionViewHolder = (VersionViewHolder) holder;
            FileVersion fileVersion = (FileVersion) values.get(position);

            versionViewHolder.size.setText(DisplayUtils.bytesToHumanReadable(fileVersion.getFileLength()));
            versionViewHolder.time.setText(DateFormat.format("HH:mm", new Date(fileVersion.getModifiedTimestamp())
                    .getTime()));

            versionViewHolder.restore.setOnClickListener(v -> versionListInterface.onRestoreClicked(fileVersion));
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object value = values.get(position);

        if (value instanceof Activity) {
            return ACTIVITY_TYPE;
        } else if (value instanceof FileVersion) {
            return VERSION_TYPE;
        } else {
            return HEADER_TYPE;
        }
    }

    protected class VersionViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.size)
        public TextView size;
        @BindView(R.id.time)
        public TextView time;
        @BindView(R.id.restore)
        public ImageView restore;

        VersionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
