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

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.ui.ExtendedListView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 *  TODO extending SherlockListFragment instead of SherlockFragment 
 */
public class ExtendedListFragment extends SherlockFragment implements OnItemClickListener {
    
    private static final String TAG = ExtendedListFragment.class.getSimpleName();

    private static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION"; 

    protected ExtendedListView mList;
    
    public void setListAdapter(ListAdapter listAdapter) {
        mList.setAdapter(listAdapter);
        mList.invalidate();
    }

    public ListView getListView() {
        return mList;
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.e(TAG, "onCreateView");
        //mList = new ExtendedListView(getActivity());
        View v = inflater.inflate(R.layout.list_fragment, null);
        mList = (ExtendedListView)(v.findViewById(R.id.list_root));
        mList.setOnItemClickListener(this);
        //mList.setEmptyView(v.findViewById(R.id.empty_list_view));     // looks like it's not a cool idea 
        mList.setDivider(getResources().getDrawable(R.drawable.uploader_list_separator));
        mList.setDividerHeight(1);

        if (savedInstanceState != null) {
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            setReferencePosition(referencePosition);
        }
        
        return v;
    }

    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log_OC.e(TAG, "onSaveInstanceState()");
        savedInstanceState.putInt(KEY_SAVED_LIST_POSITION, getReferencePosition());
    }

    
    /**
     * Calculates the position of the item that will be used as a reference to reposition the visible items in the list when
     * the device is turned to other position. 
     * 
     * THe current policy is take as a reference the visible item in the center of the screen.  
     * 
     * @return      The position in the list of the visible item in the center of the screen.
     */
    protected int getReferencePosition() {
        if (mList != null) {
            return (mList.getFirstVisiblePosition() + mList.getLastVisiblePosition()) / 2;
        } else {
            return 0;
        }
    }

    
    /**
     * Sets the visible part of the list from the reference position.
     * 
     * @param   position    Reference position previously returned by {@link LocalFileListFragment#getReferencePosition()}
     */
    protected void setReferencePosition(int position) {
        if (mList != null) {
            mList.setAndCenterSelection(position);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // to be @overriden  
    }

    
}
