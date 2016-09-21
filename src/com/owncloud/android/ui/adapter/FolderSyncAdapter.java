/**
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.owncloud.android.R;

import java.util.ArrayList;

/**
 * Adapter to display all auto-synced folders and/or instant upload media folders.
 */
public class FolderSyncAdapter extends SectionedRecyclerViewAdapter<FolderSyncAdapter.MainViewHolder>
        implements View.OnClickListener, View.OnTouchListener {

    private static final String TAG = FolderSyncAdapter.class.getSimpleName();

    private final Context mContext;
    private final int mGridWidth;
    private final ClickListener mListener;
    private final ArrayList<Object> mCategories;
    private final RecyclerView mRecyclerView;

    public FolderSyncAdapter(Context context, int gridWidth, ClickListener listener, RecyclerView recyclerView) {
        mContext = context;
        mGridWidth = gridWidth * 2;
        mListener = listener;
        mCategories = new ArrayList<>();
        mRecyclerView = recyclerView;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public int getSectionCount() {
        return 0;
    }

    @Override
    public int getItemCount(int section) {
        return 0;
    }

    @Override
    public void onBindHeaderViewHolder(MainViewHolder holder, int section) {

    }

    @Override
    public void onBindViewHolder(MainViewHolder holder, int section, int relativePosition, int absolutePosition) {

    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    public interface ClickListener {
        void onClick(View view, int section, int relative, int absolute);
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {

        public MainViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
            title = (TextView) itemView.findViewById(R.id.title);
            menuButton = (ImageButton) itemView.findViewById(R.id.syncStatusButton);
            mSyncStatusButton = (ImageButton) itemView.findViewById(R.id.settingsButton);
        }

        final ImageView image;
        final TextView title;
        final ImageButton menuButton;
        final ImageButton mSyncStatusButton;
    }
}
