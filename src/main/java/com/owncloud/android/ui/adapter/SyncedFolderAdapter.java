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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.nextcloud.client.core.Clock;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Adapter to display all auto-synced folders and/or instant upload media folders.
 */
public class SyncedFolderAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private final Context context;
    private final Clock clock;
    private final int gridWidth;
    private final int gridTotal;
    private final ClickListener clickListener;
    private final List<SyncedFolderDisplayItem> syncFolderItems;
    private final List<SyncedFolderDisplayItem> filteredSyncFolderItems;
    private final boolean light;
    private static final int VIEW_TYPE_EMPTY = Integer.MAX_VALUE;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_HEADER = 2;
    private static final int VIEW_TYPE_FOOTER = 3;
    private boolean hideItems;

    public SyncedFolderAdapter(Context context, Clock clock, int gridWidth, ClickListener listener, boolean light) {
        this.context = context;
        this.clock = clock;
        this.gridWidth = gridWidth;
        gridTotal = gridWidth * 2;
        clickListener = listener;
        syncFolderItems = new ArrayList<>();
        filteredSyncFolderItems = new ArrayList<>();
        this.light = light;
        this.hideItems = true;

        shouldShowHeadersForEmptySections(true);
        shouldShowFooters(true);
    }

    public void toggleHiddenItemsVisibility() {
        hideItems = !hideItems;
        filteredSyncFolderItems.clear();
        filteredSyncFolderItems.addAll(filterHiddenItems(syncFolderItems, hideItems));
        notifyDataSetChanged();
    }

    public void setSyncFolderItems(List<SyncedFolderDisplayItem> syncFolderItems) {
        this.syncFolderItems.clear();
        this.syncFolderItems.addAll(syncFolderItems);

        this.filteredSyncFolderItems.clear();
        this.filteredSyncFolderItems.addAll(filterHiddenItems(this.syncFolderItems, hideItems));
    }

    public void setSyncFolderItem(int location, SyncedFolderDisplayItem syncFolderItem) {
        if (hideItems && syncFolderItem.isHidden() && filteredSyncFolderItems.contains(syncFolderItem)) {
            filteredSyncFolderItems.remove(location);
        } else {
            if (filteredSyncFolderItems.contains(syncFolderItem)) {
                filteredSyncFolderItems.set(filteredSyncFolderItems.indexOf(syncFolderItem), syncFolderItem);
            } else {
                filteredSyncFolderItems.add(syncFolderItem);
            }
        }

        if (syncFolderItems.contains(syncFolderItem)) {
            syncFolderItems.set(syncFolderItems.indexOf(syncFolderItem), syncFolderItem);
        } else {
            syncFolderItems.add(syncFolderItem);
        }

        notifyDataSetChanged();
    }

    public void addSyncFolderItem(SyncedFolderDisplayItem syncFolderItem) {
        syncFolderItems.add(syncFolderItem);

        // add item for display when either all items should be shown (!hideItems)
        // or if item should be shown (!.isHidden())
        if (!hideItems || !syncFolderItem.isHidden()) {
            filteredSyncFolderItems.add(syncFolderItem);
            notifyDataSetChanged();
        }
    }

    public void removeItem(int section) {
        if (filteredSyncFolderItems.contains(syncFolderItems.get(section))) {
            filteredSyncFolderItems.remove(syncFolderItems.get(section));
            notifyDataSetChanged();
        }
        syncFolderItems.remove(section);
    }

    /**
     * Filter for hidden items
     *
     * @param items Collection of items to filter
     * @return Non-hidden items
     */
    private List<SyncedFolderDisplayItem> filterHiddenItems(List<SyncedFolderDisplayItem> items, boolean hide) {
        if (!hide) {
            return items;
        } else {
            List<SyncedFolderDisplayItem> result = new ArrayList<>();

            for (SyncedFolderDisplayItem item : items) {
                if (!item.isHidden() && !result.contains(item)) {
                    result.add(item);
                }
            }

            return result;
        }
    }

    @Override
    public int getSectionCount() {
        if (filteredSyncFolderItems.size() > 0) {
            return filteredSyncFolderItems.size() + 1;
        } else {
            return 0;
        }
    }

    public int getUnfilteredSectionCount() {
        if (syncFolderItems.size() > 0) {
            return syncFolderItems.size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getItemCount(int section) {
        if (section < filteredSyncFolderItems.size()) {
            List<String> filePaths = filteredSyncFolderItems.get(section).getFilePaths();

            if (filePaths != null) {
                return filteredSyncFolderItems.get(section).getFilePaths().size();
            } else {
                return 1;
            }
        } else {
            return 1;
        }
    }

    public SyncedFolderDisplayItem get(int section) {
        return filteredSyncFolderItems.get(section);
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        if (isLastSection(section)) {
            return VIEW_TYPE_EMPTY;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public int getHeaderViewType(int section) {
        if (isLastSection(section)) {
            return VIEW_TYPE_EMPTY;
        } else {
            return VIEW_TYPE_HEADER;
        }
    }

    @Override
    public int getFooterViewType(int section) {
        if (isLastSection(section) && showFooter()) {
            return VIEW_TYPE_FOOTER;
        } else {
            // only show footer after last item and only if folders have been hidden
            return VIEW_TYPE_EMPTY;
        }
    }

    private boolean showFooter() {
        return syncFolderItems.size() > filteredSyncFolderItems.size();
    }

    /**
     * returns the section of a synced folder for the given local path and type.
     *
     * @param localPath the local path of the synced folder
     * @param type      the of the synced folder
     * @return the section index of the looked up synced folder, <code>-1</code> if not present
     */
    public int getSectionByLocalPathAndType(String localPath, int type) {
        for (int i = 0; i < filteredSyncFolderItems.size(); i++) {
            if (filteredSyncFolderItems.get(i).getLocalPath().equalsIgnoreCase(localPath) &&
                filteredSyncFolderItems.get(i).getType().getId().equals(type)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder commonHolder, final int section, boolean expanded) {
        if (section < filteredSyncFolderItems.size()) {
            HeaderViewHolder holder = (HeaderViewHolder) commonHolder;
            holder.mainHeaderContainer.setVisibility(View.VISIBLE);

            holder.title.setText(filteredSyncFolderItems.get(section).getFolderName());

            if (MediaFolderType.VIDEO == filteredSyncFolderItems.get(section).getType()) {
                holder.type.setImageResource(R.drawable.video_32dp);
            } else if (MediaFolderType.IMAGE == filteredSyncFolderItems.get(section).getType()) {
                holder.type.setImageResource(R.drawable.image_32dp);
            } else {
                holder.type.setImageResource(R.drawable.folder_star_32dp);
            }

            holder.syncStatusButton.setVisibility(View.VISIBLE);
            holder.syncStatusButton.setTag(section);
            holder.syncStatusButton.setOnClickListener(v -> {
                filteredSyncFolderItems.get(section).setEnabled(
                    !filteredSyncFolderItems.get(section).isEnabled(),
                    clock.getCurrentTime()
                );
                setSyncButtonActiveIcon(holder.syncStatusButton, filteredSyncFolderItems.get(section).isEnabled());
                clickListener.onSyncStatusToggleClick(section, filteredSyncFolderItems.get(section));
            });
            setSyncButtonActiveIcon(holder.syncStatusButton, filteredSyncFolderItems.get(section).isEnabled());

            if (light) {
                holder.menuButton.setVisibility(View.GONE);
            } else {
                holder.menuButton.setVisibility(View.VISIBLE);
                holder.menuButton.setTag(section);
                holder.menuButton.setOnClickListener(v -> onOverflowIconClicked(section,
                                                                                filteredSyncFolderItems.get(section),
                                                                                v));
            }
        }
    }

    private void onOverflowIconClicked(int section, SyncedFolderDisplayItem item, View view) {
        ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.CustomPopupTheme);
        PopupMenu popup = new PopupMenu(ctw, view);
        popup.inflate(R.menu.synced_folders_adapter);
        popup.setOnMenuItemClickListener(i -> optionsItemSelected(i, section, item));
        popup.getMenu()
            .findItem(R.id.action_auto_upload_folder_toggle_visibility)
            .setChecked(item.isHidden());

        popup.show();
    }

    private boolean optionsItemSelected(MenuItem menuItem, int section, SyncedFolderDisplayItem item) {
        if (menuItem.getItemId() == R.id.action_auto_upload_folder_toggle_visibility) {
            clickListener.onVisibilityToggleClick(section, item);
        } else {
            // default: R.id.action_create_custom_folder
            clickListener.onSyncFolderSettingsClick(section, item);
        }
        return true;
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {
        if (isLastSection(section) && showFooter()) {
            FooterViewHolder footerHolder = (FooterViewHolder) holder;
            footerHolder.title.setOnClickListener(v -> toggleHiddenItemsVisibility());
            footerHolder.title.setText(
                context.getResources().getQuantityString(
                    R.plurals.synced_folders_show_hidden_folders,
                    getHiddenFolderCount(),
                    getHiddenFolderCount()
                )
            );
        }
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder commonHolder, int section, int relativePosition,
                                 int absolutePosition) {
        if (section < filteredSyncFolderItems.size() && filteredSyncFolderItems.get(section).getFilePaths() != null) {
            MainViewHolder holder = (MainViewHolder) commonHolder;

            File file = new File(filteredSyncFolderItems.get(section).getFilePaths().get(relativePosition));

            ThumbnailsCacheManager.MediaThumbnailGenerationTask task =
                    new ThumbnailsCacheManager.MediaThumbnailGenerationTask(holder.image, context);

            ThumbnailsCacheManager.AsyncMediaThumbnailDrawable asyncDrawable =
                    new ThumbnailsCacheManager.AsyncMediaThumbnailDrawable(
                        context.getResources(),
                        ThumbnailsCacheManager.mDefaultImg
                    );
            holder.image.setImageDrawable(asyncDrawable);

            task.execute(file);

            // set proper tag
            holder.image.setTag(file.hashCode());

            holder.itemView.setTag(relativePosition % gridWidth);

            if (filteredSyncFolderItems.get(section).getNumberOfFiles() > gridTotal && relativePosition >= gridTotal - 1) {
                holder.counterValue.setText(String.format(Locale.US, "%d",
                                                          filteredSyncFolderItems.get(section).getNumberOfFiles() - gridTotal));
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
        } else if (viewType == VIEW_TYPE_FOOTER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.synced_folders_footer, parent, false);
            return new FooterViewHolder(v);
        } else if (viewType == VIEW_TYPE_EMPTY) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.synced_folders_empty, parent, false);
            return new EmptyViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_sync_item, parent, false);
            return new MainViewHolder(v);
        }
    }

    private boolean isLastSection(int section) {
        return section >= getSectionCount() - 1;
    }

    public int getHiddenFolderCount() {
        if (syncFolderItems != null && filteredSyncFolderItems != null) {
            return syncFolderItems.size() - filteredSyncFolderItems.size();
        } else {
            return 0;
        }
    }

    public interface ClickListener {
        void onSyncStatusToggleClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem);
        void onSyncFolderSettingsClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem);
        void onVisibilityToggleClick(int section, SyncedFolderDisplayItem item);
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

    static class FooterViewHolder extends SectionedViewHolder {
        @BindView(R.id.footer_text)
        public TextView title;

        private FooterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class EmptyViewHolder extends SectionedViewHolder {
        private EmptyViewHolder(View itemView) {
            super(itemView);
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
            syncStatusButton.setImageDrawable(ThemeDrawableUtils.tintDrawable(R.drawable.ic_cloud_sync_on,
                                                                              ThemeColorUtils.primaryColor(context, true)));
        } else {
            syncStatusButton.setImageResource(R.drawable.ic_cloud_sync_off);
        }
    }
}
