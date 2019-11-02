/*
 *   Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.nextcloud.client.core.Clock;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Adapter to display all auto-synced folders and/or instant upload media folders.
 */
public class SyncedFolderAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private final Context mContext;
    private final Clock clock;
    private final int mGridWidth;
    private final int mGridTotal;
    private final ClickListener mListener;
    private final List<SyncedFolderDisplayItem> mSyncFolderItems;
    private final boolean mLight;

    public SyncedFolderAdapter(Context context, Clock clock, int gridWidth, ClickListener listener, boolean light) {
        mContext = context;
        this.clock = clock;
        mGridWidth = gridWidth;
        mGridTotal = gridWidth * 2;
        mListener = listener;
        mSyncFolderItems = new ArrayList<>();
        mLight = light;

        shouldShowHeadersForEmptySections(true);
    }

    public void setSyncFolderItems(List<SyncedFolderDisplayItem> syncFolderItems) {
        mSyncFolderItems.clear();
        mSyncFolderItems.addAll(syncFolderItems);
    }

    public void setSyncFolderItem(int location, SyncedFolderDisplayItem syncFolderItem) {
        mSyncFolderItems.set(location, syncFolderItem);
        notifyDataSetChanged();
    }

    public void addSyncFolderItem(SyncedFolderDisplayItem syncFolderItem) {
        mSyncFolderItems.add(syncFolderItem);
        notifyDataSetChanged();
    }

    public void removeItem(int section) {
        mSyncFolderItems.remove(section);
        notifyDataSetChanged();
    }

    @Override
    public int getSectionCount() {
        return mSyncFolderItems.size();
    }

    @Override
    public int getItemCount(int section) {
        List<String> filePaths = mSyncFolderItems.get(section).getFilePaths();

        if (filePaths != null) {
            return mSyncFolderItems.get(section).getFilePaths().size();
        } else {
            return 1;
        }
    }

    public SyncedFolderDisplayItem get(int section) {
        return mSyncFolderItems.get(section);
    }

    /**
     * returns the section of a synced folder for the given local path and type.
     *
     * @param localPath the local path of the synced folder
     * @param type      the of the synced folder
     * @return the section index of the looked up synced folder, <code>-1</code> if not present
     */
    public int getSectionByLocalPathAndType(String localPath, int type) {
        for (int i = 0; i < mSyncFolderItems.size(); i++) {
            if (mSyncFolderItems.get(i).getLocalPath().equalsIgnoreCase(localPath) &&
                    mSyncFolderItems.get(i).getType().getId().equals(type)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder commonHolder, final int section, boolean expanded) {
        HeaderViewHolder holder = (HeaderViewHolder) commonHolder;

        holder.mainHeaderContainer.setVisibility(View.VISIBLE);

        holder.title.setText(mSyncFolderItems.get(section).getFolderName());

        if (MediaFolderType.VIDEO == mSyncFolderItems.get(section).getType()) {
            holder.type.setImageResource(R.drawable.video_32dp);
        } else if (MediaFolderType.IMAGE == mSyncFolderItems.get(section).getType()) {
            holder.type.setImageResource(R.drawable.image_32dp);
        } else {
            holder.type.setImageResource(R.drawable.folder_star_32dp);
        }

        holder.syncStatusButton.setVisibility(View.VISIBLE);
        holder.syncStatusButton.setTag(section);
        holder.syncStatusButton.setOnClickListener(v -> {
            mSyncFolderItems.get(section).setEnabled(!mSyncFolderItems.get(section).isEnabled(), clock.getCurrentTime());
            setSyncButtonActiveIcon(holder.syncStatusButton, mSyncFolderItems.get(section).isEnabled());
            mListener.onSyncStatusToggleClick(section, mSyncFolderItems.get(section));
        });
        setSyncButtonActiveIcon(holder.syncStatusButton, mSyncFolderItems.get(section).isEnabled());

        holder.syncStatusButton.setVisibility(View.VISIBLE);
        holder.syncStatusButton.setTag(section);
        holder.syncStatusButton.setOnClickListener(v -> {
            mSyncFolderItems.get(section).setEnabled(!mSyncFolderItems.get(section).isEnabled(), clock.getCurrentTime());
            setSyncButtonActiveIcon(holder.syncStatusButton, mSyncFolderItems.get(section).isEnabled());
            mListener.onSyncStatusToggleClick(section, mSyncFolderItems.get(section));
        });
        setSyncButtonActiveIcon(holder.syncStatusButton, mSyncFolderItems.get(section).isEnabled());

        if (mLight) {
            holder.menuButton.setVisibility(View.GONE);
        } else {
            holder.menuButton.setVisibility(View.VISIBLE);
            holder.menuButton.setTag(section);
            holder.menuButton.setOnClickListener(v -> mListener.onSyncFolderSettingsClick(section,
                    mSyncFolderItems.get(section)));
        }
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {
        // not needed
    }


    @Override
    public void onBindViewHolder(SectionedViewHolder commonHolder, int section, int relativePosition,
                                 int absolutePosition) {
        if (mSyncFolderItems.get(section).getFilePaths() != null) {
            MainViewHolder holder = (MainViewHolder) commonHolder;

            File file = new File(mSyncFolderItems.get(section).getFilePaths().get(relativePosition));

            ThumbnailsCacheManager.MediaThumbnailGenerationTask task =
                    new ThumbnailsCacheManager.MediaThumbnailGenerationTask(holder.image, mContext);

            ThumbnailsCacheManager.AsyncMediaThumbnailDrawable asyncDrawable =
                    new ThumbnailsCacheManager.AsyncMediaThumbnailDrawable(
                            mContext.getResources(),
                            ThumbnailsCacheManager.mDefaultImg,
                            task
                    );
            holder.image.setImageDrawable(asyncDrawable);

            task.execute(file);

            // set proper tag
            holder.image.setTag(file.hashCode());

            holder.itemView.setTag(relativePosition % mGridWidth);

            if (mSyncFolderItems.get(section).getNumberOfFiles() > mGridTotal && relativePosition >= mGridTotal - 1) {
                holder.counterValue.setText(String.format(Locale.US, "%d",
                    mSyncFolderItems.get(section).getNumberOfFiles() - mGridTotal));
                holder.counterBar.setVisibility(View.VISIBLE);
                holder.thumbnailDarkener.setVisibility(View.VISIBLE);
            } else {
                holder.counterBar.setVisibility(View.GONE);
                holder.thumbnailDarkener.setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    @Override
    public SectionedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.synced_folders_item_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_sync_item, parent, false);
            return new MainViewHolder(v);
        }
    }

    public interface ClickListener {
        void onSyncStatusToggleClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem);
        void onSyncFolderSettingsClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem);
    }

    static class HeaderViewHolder extends SectionedViewHolder {
        @BindView(R.id.header_container)
        public RelativeLayout mainHeaderContainer;

        @BindView(R.id.type)
        public ImageView type;

        @BindView(R.id.title)
        public TextView title;

        @BindView(R.id.syncStatusButton)
        public ImageButton syncStatusButton;

        @BindView(R.id.settingsButton)
        public ImageButton menuButton;

        private HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class MainViewHolder extends SectionedViewHolder {
        @BindView(R.id.thumbnail)
        public ImageView image;

        @BindView(R.id.counterLayout)
        public LinearLayout counterBar;

        @BindView(R.id.counter)
        public TextView counterValue;

        @BindView(R.id.thumbnailDarkener)
        public ImageView thumbnailDarkener;

        private MainViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private void setSyncButtonActiveIcon(ImageButton syncStatusButton, boolean enabled) {
        if (enabled) {
            syncStatusButton.setImageDrawable(ThemeUtils.tintDrawable(R.drawable.ic_cloud_sync_on,
                    ThemeUtils.primaryColor(mContext)));
        } else {
            syncStatusButton.setImageResource(R.drawable.ic_cloud_sync_off);
        }
    }
}
