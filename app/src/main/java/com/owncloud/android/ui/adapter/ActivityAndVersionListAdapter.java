/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.databinding.VersionListItemBinding;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.interfaces.VersionListInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ActivityAndVersionListAdapter extends ActivityListAdapter {
    private static final int VERSION_TYPE = 102;

    private final VersionListInterface.View versionListInterface;

    public ActivityAndVersionListAdapter(
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ActivityListInterface activityListInterface,
        VersionListInterface.View versionListInterface,
        ClientFactory clientFactory,
        ViewThemeUtils viewThemeUtils
                                        ) {
        super(context,
              currentAccountProvider,
              activityListInterface,
              clientFactory,
              true,
              viewThemeUtils);

        this.versionListInterface = versionListInterface;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setActivityAndVersionItems(List<Object> items, NextcloudClient newClient, boolean clear) {
        if (client == null) {
            client = newClient;
        }
        if (clear) {
            values.clear();
            items.sort((o1, o2) -> {
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

            if (item instanceof Activity activity) {
                time = getHeaderDateString(context, activity.getDatetime().getTime()).toString();
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
        if (viewType == VERSION_TYPE) {
            return new VersionViewHolder(VersionListItemBinding.inflate(LayoutInflater.from(parent.getContext())));
        }

        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VersionViewHolder versionViewHolder) {
            FileVersion fileVersion = (FileVersion) values.get(position);

            versionViewHolder.binding.size.setText(DisplayUtils.bytesToHumanReadable(fileVersion.getFileLength()));
            versionViewHolder.binding.time.setText(
                DateFormat.format("HH:mm", new Date(fileVersion.getModifiedTimestamp()).getTime())
            );

            versionViewHolder.binding.restore.setOnClickListener(
                v -> versionListInterface.onRestoreClicked(fileVersion)
            );
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

        VersionListItemBinding binding;

        VersionViewHolder(VersionListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
