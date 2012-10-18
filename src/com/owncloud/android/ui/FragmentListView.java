package com.owncloud.android.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.R;
import com.owncloud.android.ui.fragment.LocalFileListFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class FragmentListView extends SherlockFragment implements
        OnItemClickListener, OnItemLongClickListener {
    protected ExtendedListView mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setListAdapter(ListAdapter listAdapter) {
        mList.setAdapter(listAdapter);
        mList.invalidate();
    }

    public ListView getListView() {
        return mList;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        //mList = new ExtendedListView(getActivity());
        View v = inflater.inflate(R.layout.list_fragment, null);
        mList = (ExtendedListView)(v.findViewById(R.id.list_root));
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);
        //mList.setEmptyView(v.findViewById(R.id.empty_list_view));     // looks like it's not a cool idea 
        mList.setDivider(getResources().getDrawable(R.drawable.uploader_list_separator));
        mList.setDividerHeight(1);
        return v;
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
            long arg3) {
        return false;
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
        return (mList.getFirstVisiblePosition() + mList.getLastVisiblePosition()) / 2;
    }

    
    /**
     * Sets the visible part of the list from the reference position.
     * 
     * @param   position    Reference position previously returned by {@link LocalFileListFragment#getReferencePosition()}
     */
    protected void setReferencePosition(int position) {
        mList.setAndCenterSelection(position);
    }

    
}
