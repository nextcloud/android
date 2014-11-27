/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.fragment;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;

/**
 *  Extending ExtendedListFragment. This allows dividing list in groups.
 */
public class ExpandableListFragment extends ExtendedListFragment 
 {
    
    protected ExpandableListView mList;
    
    public void setListAdapter(ExpandableListAdapter listAdapter) {
        mList.setAdapter(listAdapter);
        mList.invalidate();
    }

    public ExpandableListView getListView() {
        return mList;
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.e(TAG, "onCreateView");
        
        View v = inflater.inflate(R.layout.list_fragment_expandable, null);
        mEmptyListMessage = (TextView) v.findViewById(R.id.empty_list_view);
        mList = (ExpandableListView)(v.findViewById(R.id.list_root));
        mList.setOnItemClickListener(this);

        mList.setDivider(getResources().getDrawable(R.drawable.uploader_list_separator));
        mList.setDividerHeight(1);

        if (savedInstanceState != null) {
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            setReferencePosition(referencePosition);
        }
        
        // Pull down refresh
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_files);
        mRefreshEmptyLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_files_emptyView);
        
        onCreateSwipeToRefresh(mRefreshLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);
        
        mList.setEmptyView(mRefreshEmptyLayout);

        return v;
    }

    }
