package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.activities.models.Activity;
import com.owncloud.android.lib.resources.files.FileVersion;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.interfaces.VersionListInterface;
import com.owncloud.android.utils.DisplayUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ActivityAndVersionListAdapter extends ActivityListAdapter {

    private static final int VERSION_TYPE = 102;
    private VersionListInterface.View versionListInterface;

    public ActivityAndVersionListAdapter(Context context, ActivityListInterface activityListInterface,
                                         VersionListInterface.View versionListInterface,
                                         FileDataStorageManager storageManager) {
        super(context, activityListInterface, storageManager, true);

        this.versionListInterface = versionListInterface;
    }

    public void setActivityAndVersionItems(List<Object> items, OwnCloudClient newClient, boolean clear) {
        if (client == null) {
            client = newClient;
        }
        if (clear) {
            values.clear();
            Collections.sort(items, (o1, o2) -> {
                long o1Date;
                long o2Date;
                if (o1 instanceof Activity) {
                    o1Date = ((Activity) o1).datetime.getTime();
                } else {
                    o1Date = ((FileVersion) o1).getModifiedTimestamp();
                }

                if (o2 instanceof Activity) {
                    o2Date = ((Activity) o2).datetime.getTime();
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
        @BindView(R.id.thumbnail)
        public ImageView thumbnail;
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
